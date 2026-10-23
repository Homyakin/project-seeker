package ru.homyakin.seeker.game.battle;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import ru.homyakin.seeker.game.battle.Step12MixedBuildMatrix.Benchmark;
import ru.homyakin.seeker.game.battle.Step12MixedBuildMatrix.Cell;
import ru.homyakin.seeker.game.battle.Step12MixedBuildMatrix.Metric;
import ru.homyakin.seeker.game.battle.simulation.CombatSimulationTeams;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.MixedBuild;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.RoleBuild;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.skill.scaling.AttackAccess;
import ru.homyakin.seeker.game.battle.targeting.TargetingTactic;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.utils.RandomUtils;

/** Сворачивает связанные бои одной ячейки в показатели смешанной сборки и опорной роли. */
final class Step12MixedBuildObservationRunner {
    private static final long GOLDEN_GAMMA = 0x9e3779b97f4a7c15L;
    private static final String TESTED_NAME = "Проверяемый участник";

    private Step12MixedBuildObservationRunner() {
    }

    static Observation observe(Cell cell, int iteration, int maxRounds) {
        final long seed = iterationSeed(cell.seed(), iteration);
        final var mixedOn = runPair(cell, World.MIXED_ON, seed, maxRounds);
        final var mixedOff = cell.comparison().activitySkill()
            .map(ignored -> runPair(cell, World.MIXED_OFF, seed, maxRounds));
        final var reference = cell.comparison().referenceBuild()
            .map(ignored -> runPair(cell, World.REFERENCE, seed, maxRounds));
        return new Observation(mixedOn, mixedOff, reference);
    }

    static long iterationSeed(long seed, int iteration) {
        if (iteration < 0) {
            throw new IllegalArgumentException("Iteration must not be negative");
        }
        var mixed = seed + GOLDEN_GAMMA * (iteration + 1L);
        mixed = (mixed ^ mixed >>> 30) * 0xbf58476d1ce4e5b9L;
        mixed = (mixed ^ mixed >>> 27) * 0x94d049bb133111ebL;
        return mixed ^ mixed >>> 31;
    }

    private static PairMeasure runPair(Cell cell, World world, long seed, int maxRounds) {
        final var first = runSide(cell, world, seed, false, maxRounds);
        final var second = runSide(cell, world, seed, true, maxRounds);
        return first.add(second);
    }

    private static PairMeasure runSide(
        Cell cell,
        World world,
        long seed,
        boolean reverse,
        int maxRounds
    ) {
        return RandomUtils.withSeed(seed, () -> {
            final var setup = setup(cell, world);
            final BattleRandom random = setup.forcedTargetId()
                .<BattleRandom>map(targetId -> new Step12OneShotBattleRandom(seed, targetId))
                .orElseGet(() -> new SeededBattleRandom(seed));
            final var result = reverse
                ? new Battle(random).process(
                    setup.teams().opponents(),
                    setup.teams().evaluatedTeam(),
                    maxRounds
                )
                : new Battle(random).process(
                    setup.teams().evaluatedTeam(),
                    setup.teams().opponents(),
                    maxRounds
                );
            return measure(
                result,
                setup.tested().id(),
                setup.teams().opponents().stream().map(BattlePersonage::id).toList(),
                setup.forcedTargetId(),
                reverse
            );
        });
    }

    private static BattleSetup setup(Cell cell, World world) {
        final var comparison = cell.comparison();
        final BattlePersonage tested = switch (world) {
            case MIXED_ON -> mixedPersonage(comparison.mixedBuild(), cell.level(), Optional.empty());
            case MIXED_OFF -> mixedPersonage(
                comparison.mixedBuild(),
                cell.level(),
                comparison.activitySkill()
            );
            case REFERENCE -> rolePersonage(
                comparison.referenceBuild().orElseThrow(),
                cell.level()
            );
        };
        final var allies = allies(comparison.benchmark(), tested, cell.level());
        final var opponents = opponents(comparison.benchmark(), cell.level());
        return new BattleSetup(
            new CombatSimulationTeams(allies.personages(), opponents),
            tested,
            allies.forcedTargetId()
        );
    }

    private static Allies allies(Benchmark benchmark, BattlePersonage tested, int level) {
        if (benchmark == Benchmark.GUARD) {
            final var ward = neutral(
                RoleBuild.RANGER_PHYSICAL,
                Position.MID,
                TargetingTactic.THREAT,
                "Подзащитный",
                level
            );
            return new Allies(List.of(
                tested,
                neutral(
                    RoleBuild.GUARDIAN,
                    Position.FRONT,
                    TargetingTactic.THREAT,
                    "Опора фронта",
                    level
                ),
                ward
            ), Optional.of(ward.id()));
        }
        return new Allies(List.of(
            neutral(
                RoleBuild.GUARDIAN,
                Position.FRONT,
                TargetingTactic.THREAT,
                "Опора фронта",
                level
            ),
            tested,
            neutral(
                RoleBuild.RANGER_PHYSICAL,
                Position.BACK,
                TargetingTactic.THREAT,
                "Опора тыла",
                level
            )
        ), Optional.empty());
    }

    private static List<BattlePersonage> opponents(Benchmark benchmark, int level) {
        return switch (benchmark) {
            case GUARD -> List.of(
                neutral(
                    RoleBuild.TACTICIAN_PHYSICAL,
                    Position.FRONT,
                    TargetingTactic.EXPLOIT_WEAKNESS,
                    "Нападающий 1",
                    level
                ),
                neutral(
                    RoleBuild.TACTICIAN_PHYSICAL,
                    Position.FRONT,
                    TargetingTactic.EXPLOIT_WEAKNESS,
                    "Нападающий 2",
                    level
                ),
                neutral(
                    RoleBuild.TACTICIAN_PHYSICAL,
                    Position.MID,
                    TargetingTactic.EXPLOIT_WEAKNESS,
                    "Нападающий 3",
                    level
                )
            );
            case PRESSURE, BURST -> List.of(
                neutralGuardian(Position.FRONT, "Стойкая цель 1", level),
                neutralGuardian(Position.MID, "Стойкая цель 2", level),
                neutralGuardian(Position.BACK, "Стойкая цель 3", level)
            );
            case ACCESS -> List.of(
                neutralGuardian(Position.FRONT, "Прикрытие 1", level),
                neutralGuardian(Position.MID, "Прикрытие 2", level),
                neutral(
                    RoleBuild.RANGER_PHYSICAL,
                    Position.BACK,
                    TargetingTactic.THREAT,
                    "Дальняя цель",
                    level
                )
            );
            case MOBILITY, RANGED, TEMPO, CONTROL -> List.of(
                neutralBruiser(Position.FRONT, TargetingTactic.THREAT, "Ближняя цель 1", level),
                neutralBruiser(Position.FRONT, TargetingTactic.THREAT, "Ближняя цель 2", level),
                neutralBruiser(Position.MID, TargetingTactic.THREAT, "Ближняя цель 3", level)
            );
        };
    }

    static BattlePersonage mixedPersonage(
        MixedBuild build,
        int level,
        Optional<ActiveEnum> disabledSkill
    ) {
        final var items = Step12SimulationFixtures.items(build, level).stream()
            .map(item -> disabledSkill.isPresent()
                && item.modifier().map(modifier -> modifier.activeEnum() == disabledSkill.get()).orElse(false)
                ? withoutModifier(item)
                : item)
            .toList();
        return personage(items, build.position(), build.targetingTactic(), TESTED_NAME);
    }

    private static BattlePersonage rolePersonage(RoleBuild build, int level) {
        return personage(
            Step12SimulationFixtures.items(build, level),
            build.position(),
            build.targetingTactic(),
            TESTED_NAME
        );
    }

    private static BattlePersonage neutralBruiser(
        Position position,
        TargetingTactic tactic,
        String name,
        int level
    ) {
        return neutral(RoleBuild.BRUISER, position, tactic, name, level);
    }

    private static BattlePersonage neutralGuardian(Position position, String name, int level) {
        return neutral(RoleBuild.GUARDIAN, position, TargetingTactic.THREAT, name, level);
    }

    private static BattlePersonage neutral(
        RoleBuild build,
        Position position,
        TargetingTactic tactic,
        String name,
        int level
    ) {
        final var items = Step12SimulationFixtures.items(build, level).stream()
            .map(Step12MixedBuildObservationRunner::withoutModifier)
            .toList();
        return personage(items, position, tactic, name);
    }

    private static Item withoutModifier(Item item) {
        return new Item(item.object(), Optional.empty(), item.rarity(), item.enhanceLevel());
    }

    private static BattlePersonage personage(
        List<Item> items,
        Position position,
        TargetingTactic tactic,
        String name
    ) {
        final var personage = BattlePersonage.forScalingSkillsV2Combat(
            items,
            position,
            Map.of(),
            Optional.of(name)
        );
        personage.setTargetingTactic(tactic);
        return personage;
    }

    private static PairMeasure measure(
        BattleResult result,
        UUID testedId,
        List<UUID> opponentIds,
        Optional<UUID> protectedId,
        boolean reverse
    ) {
        long normalDamage = 0;
        long normalAttempts = 0;
        long farNormalAttempts = 0;
        long penetrationNormalDamage = 0;
        long accumulationDamage = 0;
        long ownTurns = 0;
        long enemyTurns = 0;
        long enemyAttempts = 0;
        long eligibleGuardAttempts = 0;
        long interceptions = 0;
        long hitAndRunMoves = 0;
        long eligibleKnockbacks = 0;
        for (final var trace : result.actionLog().traceEvents()) {
            if (trace instanceof BattleTraceEvent.TurnFinished turn) {
                if (turn.personageId().equals(testedId) && turn.actionPerformed()) {
                    ownTurns++;
                } else if (opponentIds.contains(turn.personageId()) && turn.actionPerformed()) {
                    enemyTurns++;
                }
            } else if (trace instanceof BattleTraceEvent.NormalAttackAttempt attempt) {
                if (attempt.attackerId().equals(testedId)) {
                    normalAttempts++;
                    normalDamage += attempt.normalDamage();
                    if (attempt.distance() >= 3 && attempt.distance() <= 4) {
                        farNormalAttempts++;
                    }
                    if (attempt.access() == AttackAccess.PENETRATION) {
                        penetrationNormalDamage += attempt.normalDamage();
                    }
                    if (attempt.access() == AttackAccess.HIT_AND_RUN
                        && attempt.lineBeforeRetreat() != attempt.lineAfterRetreat()) {
                        hitAndRunMoves++;
                    }
                    accumulationDamage += attempt.dischargeDamage();
                    if (attempt.critical() && !attempt.dodged() && !attempt.targetDefeated()) {
                        eligibleKnockbacks++;
                    }
                } else if (opponentIds.contains(attempt.attackerId())) {
                    enemyAttempts++;
                    if (protectedId.filter(attempt.originalTargetId()::equals).isPresent()) {
                        eligibleGuardAttempts++;
                        if (!attempt.originalTargetId().equals(attempt.finalTargetId())
                            && attempt.finalTargetId().equals(testedId)
                            && attempt.normalDamage() > 0) {
                            interceptions++;
                        }
                    }
                }
            }
        }

        long penetrationSkillDamage = 0;
        long doubleAttackDamage = 0;
        long initiativeRemoved = 0;
        long knockbackMoves = 0;
        for (final var event : result.actionLog().events()) {
            if (event instanceof BattleEvent.ScalingSkillDamage damage
                && damage.sourceId().equals(testedId)) {
                if (damage.skill() == ActiveEnum.PENETRATION) {
                    penetrationSkillDamage += damage.damageTaken();
                } else if (damage.skill() == ActiveEnum.DOUBLE_ATTACK) {
                    doubleAttackDamage += damage.damageTaken();
                }
            } else if (event instanceof BattleEvent.InitiativeDelayed delay
                && delay.sourceId().equals(testedId)
                && delay.skill() == ActiveEnum.TEMPO_BREAK) {
                initiativeRemoved += delay.amount();
            } else if (event instanceof BattleEvent.PersonageForcedMove movement
                && movement.sourceId().equals(testedId)
                && movement.skill() == ActiveEnum.KNOCKBACK) {
                knockbackMoves++;
            }
        }

        return new PairMeasure(
            normalDamage,
            normalDamage + doubleAttackDamage,
            penetrationNormalDamage + penetrationSkillDamage,
            accumulationDamage,
            normalAttempts,
            farNormalAttempts,
            ownTurns,
            enemyTurns,
            enemyAttempts,
            eligibleGuardAttempts,
            interceptions,
            hitAndRunMoves,
            initiativeRemoved,
            knockbackMoves,
            eligibleKnockbacks,
            reverse ? (result.firstWin() ? 0 : 1) : (result.firstWin() ? 1 : 0),
            result.firstWin() ? 1 : 0,
            1,
            knockbackMoves <= eligibleKnockbacks
        );
    }

    enum World {
        MIXED_ON,
        MIXED_OFF,
        REFERENCE,
    }

    record Observation(
        PairMeasure mixedOn,
        Optional<PairMeasure> mixedOff,
        Optional<PairMeasure> reference
    ) {
        Observation {
            Objects.requireNonNull(mixedOn, "mixedOn");
            Objects.requireNonNull(mixedOff, "mixedOff");
            Objects.requireNonNull(reference, "reference");
        }
    }

    record PairMeasure(
        long normalDamage,
        long rangedSustainedDamage,
        long penetrationDamage,
        long accumulationDamage,
        long normalAttempts,
        long farNormalAttempts,
        long ownTurns,
        long opponentTurns,
        long opponentAttempts,
        long eligibleGuardAttempts,
        long interceptions,
        long hitAndRunMoves,
        long initiativeRemoved,
        long knockbackMoves,
        long eligibleKnockbacks,
        int evaluatedWins,
        int firstSideWins,
        int battles,
        boolean knockbackLimitKept
    ) {
        PairMeasure add(PairMeasure other) {
            return new PairMeasure(
                normalDamage + other.normalDamage,
                rangedSustainedDamage + other.rangedSustainedDamage,
                penetrationDamage + other.penetrationDamage,
                accumulationDamage + other.accumulationDamage,
                normalAttempts + other.normalAttempts,
                farNormalAttempts + other.farNormalAttempts,
                ownTurns + other.ownTurns,
                opponentTurns + other.opponentTurns,
                opponentAttempts + other.opponentAttempts,
                eligibleGuardAttempts + other.eligibleGuardAttempts,
                interceptions + other.interceptions,
                hitAndRunMoves + other.hitAndRunMoves,
                initiativeRemoved + other.initiativeRemoved,
                knockbackMoves + other.knockbackMoves,
                eligibleKnockbacks + other.eligibleKnockbacks,
                evaluatedWins + other.evaluatedWins,
                firstSideWins + other.firstSideWins,
                battles + other.battles,
                knockbackLimitKept && other.knockbackLimitKept
            );
        }

        double metric(Metric metric) {
            return switch (metric) {
                case GUARD_INTERCEPTIONS_PER_ENEMY_ATTEMPT -> ratio(interceptions, eligibleGuardAttempts);
                case NORMAL_DAMAGE_PER_TURN -> ratio(normalDamage, ownTurns);
                case HIT_AND_RUN_MOVES_PER_TURN -> ratio(hitAndRunMoves, ownTurns);
                case PENETRATION_DAMAGE_PER_TURN -> ratio(penetrationDamage, ownTurns);
                case RANGED_SUSTAINED_DAMAGE_PER_TURN -> ratio(rangedSustainedDamage, ownTurns);
                case ACCUMULATION_DAMAGE_PER_TURN -> ratio(accumulationDamage, ownTurns);
                case INITIATIVE_REMOVED_PER_TURN -> ratio(initiativeRemoved, ownTurns);
                case KNOCKBACK_MOVES_PER_TURN -> ratio(knockbackMoves, ownTurns);
            };
        }

        boolean hasDenominator(Metric metric) {
            return switch (metric) {
                case GUARD_INTERCEPTIONS_PER_ENEMY_ATTEMPT -> eligibleGuardAttempts > 0;
                case NORMAL_DAMAGE_PER_TURN,
                     HIT_AND_RUN_MOVES_PER_TURN,
                     PENETRATION_DAMAGE_PER_TURN,
                     RANGED_SUSTAINED_DAMAGE_PER_TURN,
                     ACCUMULATION_DAMAGE_PER_TURN,
                     INITIATIVE_REMOVED_PER_TURN,
                     KNOCKBACK_MOVES_PER_TURN -> ownTurns > 0;
            };
        }

        double opponentMovementOnlyTurnShare() {
            return ratio(Math.max(0, opponentTurns - opponentAttempts), opponentTurns);
        }

        double farNormalAttemptShare() {
            return ratio(farNormalAttempts, normalAttempts);
        }

        double evaluatedWinRate() {
            return ratio(evaluatedWins, battles);
        }

        double firstSideWinRate() {
            return ratio(firstSideWins, battles);
        }

        private static double ratio(long numerator, long denominator) {
            return denominator == 0 ? 0 : (double) numerator / denominator;
        }
    }

    private record Allies(List<BattlePersonage> personages, Optional<UUID> forcedTargetId) {
    }

    private record BattleSetup(
        CombatSimulationTeams teams,
        BattlePersonage tested,
        Optional<UUID> forcedTargetId
    ) {
    }
}
