package ru.homyakin.seeker.game.battle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.battle.Step12RepresentativeBuilds.Build;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.MixedBuild;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.RoleBuild;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.targeting.TargetingTactic;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemAttack;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.ItemProgressionVersion;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.utils.RandomUtils;

class Step12RepresentativeRunnerTest {
    private static final long SEED = 2_026_102_301L;
    private static final int MAX_ROUNDS = 10_000;
    private static final BattleRandom MINIMUM_RANDOM = (sequence, minimum, maximum) -> minimum;

    @Test
    void completeObservationIncludingParticipantIdsIsReproducible() {
        final var left = List.of(role(RoleBuild.GUARDIAN), mixed(MixedBuild.BLOCKER));
        final var right = List.of(role(RoleBuild.ASSASSIN), mixed(MixedBuild.ARTILLERIST));
        for (final boolean reverse : List.of(false, true)) {
            final var first = Step12RepresentativeBattleRunner.observe(
                left, right, 10, SEED, reverse, MAX_ROUNDS, Set.of(), Set.of()
            );
            final var repeated = Step12RepresentativeBattleRunner.observe(
                left, right, 10, SEED, reverse, MAX_ROUNDS, Set.of(), Set.of()
            );
            Assertions.assertEquals(first, repeated);
            Assertions.assertFalse(first.events().isEmpty());
            Assertions.assertFalse(first.traces().isEmpty());
        }
    }

    @Test
    void boundedRunnerMatchesProductionForFreshCanonicalTeamsInBothOrders() {
        final var cases = List.of(
            new Teams(
                List.of(role(RoleBuild.BRUISER)),
                List.of(role(RoleBuild.GUARDIAN)),
                Set.of(), Set.of()
            ),
            new Teams(
                List.of(role(RoleBuild.GUARDIAN), role(RoleBuild.TACTICIAN_PHYSICAL), role(RoleBuild.RANGER_PHYSICAL)),
                List.of(role(RoleBuild.GUARDIAN), role(RoleBuild.ASSASSIN), role(RoleBuild.RANGER_MAGICAL)),
                Set.of(ActiveEnum.TEMPO_BREAK), Set.of(ActiveEnum.GUARD)
            ),
            new Teams(
                List.of(mixed(MixedBuild.BLOCKER), mixed(MixedBuild.RAIDER)),
                List.of(mixed(MixedBuild.KEEPER), mixed(MixedBuild.ARTILLERIST)),
                Set.of(), Set.of()
            ),
            new Teams(
                List.of(Step12RepresentativeBuilds.hand(
                    "NO_SKILLS_LEFT", List.of("sword", "shield"), "CLOTH", Position.FRONT, TargetingTactic.THREAT
                )),
                List.of(Step12RepresentativeBuilds.hand(
                    "NO_SKILLS_RIGHT", List.of("halberd"), "LEATHER", Position.MID, TargetingTactic.THREAT
                )),
                Set.of(), Set.of()
            )
        );
        for (final var teams : cases) {
            for (final long seed : List.of(SEED, SEED + 1)) {
                for (final int level : List.of(0, 10)) {
                    for (final boolean reverse : List.of(false, true)) {
                        final var actual = Step12RepresentativeBattleRunner.observe(
                            teams.left(), teams.right(), level, seed, reverse, MAX_ROUNDS,
                            teams.disabledLeft(), teams.disabledRight()
                        );
                        final var expected = production(teams, level, seed, reverse);
                        final var result = expected.result();
                        final boolean anyLeftAlive = expected.left().stream().anyMatch(BattlePersonage::isAlive);
                        final boolean anyRightAlive = expected.right().stream().anyMatch(BattlePersonage::isAlive);
                        final var expectedOutcome = !anyLeftAlive && !anyRightAlive
                            ? "DRAW"
                            : result.firstWin() != reverse ? "WIN" : "LOSS";
                        Assertions.assertAll(
                            () -> Assertions.assertEquals(expectedOutcome, actual.outcome()),
                            () -> Assertions.assertEquals(result.rounds(), actual.rounds()),
                            () -> Assertions.assertEquals(result.actionLog().events(), actual.events()),
                            () -> Assertions.assertEquals(result.actionLog().traceEvents(), actual.traces()),
                            () -> Assertions.assertEquals(
                                Step12RepresentativeBattleRunner.measure(
                                    expected.left(), result.actionLog().events(), result.actionLog().traceEvents()
                                ),
                                actual.left()
                            ),
                            () -> Assertions.assertEquals(
                                Step12RepresentativeBattleRunner.measure(
                                    expected.right(), result.actionLog().events(), result.actionLog().traceEvents()
                                ),
                                actual.right()
                            ),
                            () -> Assertions.assertEquals(initialPositions(teams, expected), actual.initialPositions()),
                            () -> Assertions.assertFalse(actual.traces().isEmpty())
                        );
                    }
                }
            }
        }
    }

    @Test
    void reversingSidesPreservesCanonicalParticipantIds() {
        final var left = List.of(role(RoleBuild.GUARDIAN));
        final var right = List.of(role(RoleBuild.ASSASSIN));
        final var normal = Step12RepresentativeBattleRunner.observe(
            left, right, 0, SEED, false, MAX_ROUNDS, Set.of(), Set.of()
        );
        final var reversed = Step12RepresentativeBattleRunner.observe(
            left, right, 0, SEED, true, MAX_ROUNDS, Set.of(), Set.of()
        );
        final var expectedIds = RandomUtils.withSeed(SEED, () -> List.of(
            left.getFirst().personage(0).id(),
            right.getFirst().personage(0).id()
        ));
        Assertions.assertAll(
            () -> Assertions.assertEquals(Set.copyOf(expectedIds), traceParticipants(normal.traces())),
            () -> Assertions.assertEquals(Set.copyOf(expectedIds), traceParticipants(reversed.traces()))
        );
    }

    @Test
    void timeoutRetainsActionsHealthAndLogsAndDoesNotBecomeALoss() {
        final var guardian = role(RoleBuild.GUARDIAN);
        final int firstActionRound = (int) Math.ceil(
            (double) CombatRules.INITIATIVE_THRESHOLD / guardian.personage(0).initiative()
        );
        final var observation = Step12RepresentativeBattleRunner.observe(
            List.of(guardian), List.of(guardian), 0, SEED, false, firstActionRound, Set.of(), Set.of()
        );
        final long initialHealth = guardian.personage(0).maxHealth();
        Assertions.assertAll(
            () -> Assertions.assertEquals("TIMEOUT", observation.outcome()),
            () -> Assertions.assertEquals(firstActionRound, observation.rounds()),
            () -> Assertions.assertEquals(1, observation.left().survivors()),
            () -> Assertions.assertEquals(1, observation.right().survivors()),
            () -> Assertions.assertTrue(observation.left().remainingHealth() > 0),
            () -> Assertions.assertTrue(observation.right().remainingHealth() > 0),
            () -> Assertions.assertTrue(observation.left().actions() > 0),
            () -> Assertions.assertTrue(observation.right().actions() > 0),
            () -> Assertions.assertTrue(observation.left().attempts() > 0),
            () -> Assertions.assertTrue(observation.right().attempts() > 0),
            () -> Assertions.assertTrue(observation.left().damageTaken() > 0),
            () -> Assertions.assertTrue(observation.right().damageTaken() > 0),
            () -> Assertions.assertEquals(
                initialHealth, observation.left().remainingHealth() + observation.left().damageTaken()
            ),
            () -> Assertions.assertEquals(
                initialHealth, observation.right().remainingHealth() + observation.right().damageTaken()
            ),
            () -> Assertions.assertFalse(observation.events().isEmpty()),
            () -> Assertions.assertFalse(observation.traces().isEmpty()),
            () -> Assertions.assertEquals(firstActionRound, observation.left().aliveTicks()),
            () -> Assertions.assertEquals(firstActionRound, observation.right().aliveTicks())
        );
    }

    @Test
    void actualDamageHealingAndInitiativeAreAttributedFromRealActions() {
        final var source = telemetryPersonage(Map.of(ActiveEnum.SELF_HEAL, 8, ActiveEnum.TEMPO_BREAK, 8));
        final var target = telemetryPersonage(Map.of());
        final var context = new BattleContext(List.of(source), List.of(target), MINIMUM_RANDOM);
        final var log = new BattleActionLog();
        target.tick(log, 1);
        target.move(context, log, 1);
        source.tick(log, 1);
        source.move(context, log, 1);

        final var sourceMeasure = Step12RepresentativeBattleRunner.measure(List.of(source), log.events(), log.traceEvents());
        final var targetMeasure = Step12RepresentativeBattleRunner.measure(List.of(target), log.events(), log.traceEvents());
        final long damageReceived = log.events().stream()
            .filter(BattleEvent.DamageReceived.class::isInstance)
            .map(BattleEvent.DamageReceived.class::cast)
            .filter(damage -> damage.targetId().equals(source.id()))
            .mapToLong(BattleEvent.DamageReceived::damageTaken).sum();
        final long healed = log.events().stream()
            .filter(BattleEvent.PersonageHealed.class::isInstance)
            .map(BattleEvent.PersonageHealed.class::cast)
            .mapToLong(BattleEvent.PersonageHealed::amount).sum();
        final long removed = log.events().stream()
            .filter(BattleEvent.InitiativeDelayed.class::isInstance)
            .map(BattleEvent.InitiativeDelayed.class::cast)
            .mapToLong(BattleEvent.InitiativeDelayed::amount).sum();
        Assertions.assertAll(
            () -> Assertions.assertTrue(damageReceived > 0),
            () -> Assertions.assertTrue(healed > 0),
            () -> Assertions.assertTrue(removed > 0),
            () -> Assertions.assertEquals(damageReceived, sourceMeasure.damageTaken()),
            () -> Assertions.assertNotEquals(source.battlePersonageStats().damageTaken(), sourceMeasure.damageTaken()),
            () -> Assertions.assertEquals(healed, sourceMeasure.healing()),
            () -> Assertions.assertEquals(damageReceived, healed),
            () -> Assertions.assertEquals(source.maxHealth(), sourceMeasure.remainingHealth()),
            () -> Assertions.assertEquals(removed, sourceMeasure.initiativeRemoved()),
            () -> Assertions.assertEquals(0, targetMeasure.initiativeRemoved()),
            () -> Assertions.assertEquals(0, targetMeasure.healing()),
            () -> Assertions.assertEquals(1, sourceMeasure.actions()),
            () -> Assertions.assertEquals(1, sourceMeasure.attempts()),
            () -> Assertions.assertEquals(0, sourceMeasure.movementTurns()),
            () -> Assertions.assertEquals(1, sourceMeasure.aliveTicks()),
            () -> Assertions.assertEquals(0, sourceMeasure.longestNoAttemptRounds())
        );
    }

    @Test
    void movementOnlyActionsAndConsecutiveRoundsWithoutAnAttemptAreMeasuredSeparately() {
        final var mover = telemetryPersonage(Map.of());
        final var target = telemetryPersonage(Map.of());
        final var context = new BattleContext(List.of(mover), List.of(target), MINIMUM_RANDOM);
        final var log = new BattleActionLog();
        context.moveBackward(mover, 2);
        for (int round = 1; round <= 3; round++) {
            mover.tick(log, round);
            mover.move(context, log, round);
        }
        final var measure = Step12RepresentativeBattleRunner.measure(List.of(mover), log.events(), log.traceEvents());
        Assertions.assertAll(
            () -> Assertions.assertEquals(3, measure.actions()),
            () -> Assertions.assertEquals(1, measure.attempts()),
            () -> Assertions.assertEquals(2, measure.movementTurns()),
            () -> Assertions.assertEquals(3, measure.aliveTicks()),
            () -> Assertions.assertEquals(2, measure.longestNoAttemptRounds())
        );
    }

    @Test
    void deathFromPeriodicDamageBeforeActionIsNotAnActionOrMovementTurn() {
        final var victim = telemetryPersonage(Map.of());
        final var source = telemetryPersonage(Map.of());
        victim.addOrReplaceScalingBleeding(new ScalingBleedingEffect(
            source, Map.of(AttackType.PIERCE, 3_000), 1, 1, 1
        ));
        final var context = new BattleContext(List.of(victim), List.of(source), MINIMUM_RANDOM);
        final var log = new BattleActionLog();
        victim.tick(log, 1);
        victim.move(context, log, 1);
        final var measure = Step12RepresentativeBattleRunner.measure(List.of(victim), log.events(), log.traceEvents());
        Assertions.assertAll(
            () -> Assertions.assertEquals(0, measure.survivors()),
            () -> Assertions.assertEquals(0, measure.actions()),
            () -> Assertions.assertEquals(0, measure.attempts()),
            () -> Assertions.assertEquals(0, measure.movementTurns()),
            () -> Assertions.assertEquals(victim.maxHealth(), measure.damageTaken()),
            () -> Assertions.assertEquals(1, measure.aliveTicks()),
            () -> Assertions.assertEquals(1, measure.longestNoAttemptRounds())
        );
    }

    private static ProductionRun production(Teams builds, int level, long seed, boolean reverse) {
        return RandomUtils.withSeed(seed, () -> {
            final var left = productionTeam(builds.left(), level, builds.disabledLeft());
            final var right = productionTeam(builds.right(), level, builds.disabledRight());
            final var result = new Battle(seed).process(reverse ? right : left, reverse ? left : right, MAX_ROUNDS);
            return new ProductionRun(result, left, right);
        });
    }

    private static List<BattlePersonage> productionTeam(List<Build> builds, int level, Set<ActiveEnum> disabled) {
        return builds.stream().map(build -> {
            final var items = build.items(level).stream().map(item -> new Item(
                item.object(),
                item.modifier().filter(modifier -> !disabled.contains(modifier.activeEnum())),
                item.rarity(),
                item.enhanceLevel()
            )).toList();
            final var personage = BattlePersonage.forScalingSkillsV2Combat(
                items, build.position(), Map.of(), Optional.of(build.code())
            );
            personage.setTargetingTactic(build.tactic());
            return personage;
        }).toList();
    }

    private static List<String> initialPositions(Teams builds, ProductionRun run) {
        final var positions = new ArrayList<String>();
        appendPositions(positions, "LEFT", builds.left(), run.left(), run.result().initState());
        appendPositions(positions, "RIGHT", builds.right(), run.right(), run.result().initState());
        return List.copyOf(positions);
    }

    private static void appendPositions(
        List<String> positions,
        String side,
        List<Build> builds,
        List<BattlePersonage> team,
        BattleInitState initState
    ) {
        for (int index = 0; index < team.size(); index++) {
            positions.add("%s:%d:%s:%s:%d".formatted(
                side, index, builds.get(index).code(), team.get(index).startPosition(),
                initState.personagesById().get(team.get(index).id()).lineIndex()
            ));
        }
    }

    private static Set<UUID> traceParticipants(List<BattleTraceEvent> traces) {
        return traces.stream().map(BattleTraceEvent::personageId).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static BattlePersonage telemetryPersonage(Map<ActiveEnum, Integer> skills) {
        final var item = new Item(new ItemObject(
            "representative-telemetry", Set.of(PersonageSlot.MAIN_HAND),
            List.of(new ItemAttack(AttackType.PIERCE, 1, 1, 100)), Optional.empty(),
            2_000, 0, 0, 0, 100, 10, 100, ItemProgressionVersion.V1, Map.of()
        ), Optional.empty(), ItemRarity.COMMON);
        return BattlePersonage.forScalingSkillsV2Combat(List.of(item), Position.FRONT, skills, Optional.empty());
    }

    private static Build role(RoleBuild build) {
        return Step12RepresentativeBuilds.role(build);
    }

    private static Build mixed(MixedBuild build) {
        return Step12RepresentativeBuilds.mixed(build);
    }

    private record Teams(List<Build> left, List<Build> right, Set<ActiveEnum> disabledLeft, Set<ActiveEnum> disabledRight) {
    }

    private record ProductionRun(BattleResult result, List<BattlePersonage> left, List<BattlePersonage> right) {
    }
}
