package ru.homyakin.seeker.game.battle;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import ru.homyakin.seeker.game.battle.simulation.CombatSimulationTeams;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.V2Build;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.V2Matchup;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.V2Placement;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.utils.RandomUtils;

/** Shared deterministic controls for the preregistered step 12 revision-two causal checks. */
final class Step12V2CausalTestSupport {
    static final long WIRING_ROOT = 2_026_091_801L;

    private Step12V2CausalTestSupport() {
    }

    static Scenario scenario(V2Matchup matchup) {
        return scenario(matchup, 0, Optional.empty());
    }

    static Scenario scenario(V2Matchup matchup, int enhanceLevel) {
        return scenario(matchup, enhanceLevel, Optional.empty());
    }

    static Scenario scenarioWithoutSkill(V2Matchup matchup, V2Build disabledBuild) {
        return scenario(matchup, 0, Optional.of(disabledBuild));
    }

    static Scenario scenarioWithoutSkill(
        V2Matchup matchup,
        int enhanceLevel,
        V2Build disabledBuild
    ) {
        return scenario(matchup, enhanceLevel, Optional.of(disabledBuild));
    }

    static Scenario scenario(
        List<V2Placement> evaluatedTeam,
        List<V2Placement> opponents,
        int enhanceLevel
    ) {
        return RandomUtils.withSeed(WIRING_ROOT, () -> {
            final var teams = new CombatSimulationTeams(
                team(evaluatedTeam, enhanceLevel, Optional.empty()),
                team(opponents, enhanceLevel, Optional.empty())
            );
            final var random = new ControlledRandom(WIRING_ROOT);
            final var context = new BattleContext(teams.evaluatedTeam(), teams.opponents(), random);
            return new Scenario(teams, context, random, new BattleActionLog());
        });
    }

    static Scenario guardianAgainstDirectSkill(int enhanceLevel) {
        return RandomUtils.withSeed(WIRING_ROOT, () -> {
            final var ward = personage(V2Build.SUPPORT, Position.FRONT, enhanceLevel, Optional.empty());
            final var guardian = personage(
                V2Build.GUARDIAN_BLUNT,
                Position.FRONT,
                enhanceLevel,
                Optional.empty()
            );
            final var thornsOwner = BattlePersonage.forScalingSkills(
                Step12SimulationFixtures.v2ItemsWithoutModifier(V2Build.BRUISER_BLUNT, enhanceLevel),
                Position.FRONT,
                Map.of(ActiveEnum.THORNS, 4)
            );
            final var teams = new CombatSimulationTeams(List.of(ward, guardian), List.of(thornsOwner));
            final var random = new ControlledRandom(WIRING_ROOT);
            final var context = new BattleContext(teams.evaluatedTeam(), teams.opponents(), random);
            return new Scenario(teams, context, random, new BattleActionLog());
        });
    }

    static Scenario scenario(
        V2Matchup matchup,
        int enhanceLevel,
        Optional<V2Build> disabledBuild
    ) {
        return RandomUtils.withSeed(WIRING_ROOT, () -> {
            final var teams = new CombatSimulationTeams(
                team(matchup.evaluatedTeam(), enhanceLevel, disabledBuild),
                team(matchup.opponents(), enhanceLevel, disabledBuild)
            );
            final var random = new ControlledRandom(WIRING_ROOT);
            final var context = new BattleContext(teams.evaluatedTeam(), teams.opponents(), random);
            return new Scenario(teams, context, random, new BattleActionLog());
        });
    }

    static Scenario duel(
        V2Build evaluated,
        Position evaluatedPosition,
        V2Build opponent,
        Position opponentPosition,
        int enhanceLevel,
        Optional<V2Build> disabledBuild
    ) {
        return RandomUtils.withSeed(WIRING_ROOT, () -> {
            final var first = personage(evaluated, evaluatedPosition, enhanceLevel, disabledBuild);
            final var second = personage(opponent, opponentPosition, enhanceLevel, disabledBuild);
            final var teams = new CombatSimulationTeams(List.of(first), List.of(second));
            final var random = new ControlledRandom(WIRING_ROOT);
            final var context = new BattleContext(teams.evaluatedTeam(), teams.opponents(), random);
            return new Scenario(teams, context, random, new BattleActionLog());
        });
    }

    static List<BattlePersonage> team(
        List<V2Placement> placements,
        int enhanceLevel,
        Optional<V2Build> disabledBuild
    ) {
        return placements.stream()
            .map(placement -> personage(
                placement.build(),
                placement.position(),
                enhanceLevel,
                disabledBuild
            ))
            .toList();
    }

    static BattlePersonage personage(
        V2Build build,
        Position position,
        int enhanceLevel,
        Optional<V2Build> disabledBuild
    ) {
        return disabledBuild.filter(build::equals).isPresent()
            ? Step12SimulationFixtures.v2PersonageWithoutModifier(build, position, enhanceLevel)
            : Step12SimulationFixtures.v2Personage(build, position, enhanceLevel);
    }

    static BattlePersonage named(List<BattlePersonage> team, V2Build build) {
        return team.stream()
            .filter(personage -> personage.name().orElseThrow().equals(build.displayName()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Build is absent from team: " + build));
    }

    static List<BattlePersonage> namedAll(List<BattlePersonage> team, V2Build build) {
        return team.stream()
            .filter(personage -> personage.name().orElseThrow().equals(build.displayName()))
            .toList();
    }

    static void setHealth(BattlePersonage personage, int health) {
        if (health <= 0 || health > personage.maxHealth()) {
            throw new IllegalArgumentException("Health must stay inside the living interval");
        }
        personage.applyVersionedSkillDamage(
            personage.health() - health,
            personage.health() - health,
            personage
        );
    }

    static void defeat(BattlePersonage personage) {
        personage.applyVersionedSkillDamage(
            personage.health(),
            personage.health(),
            personage
        );
    }

    static <T extends BattleEvent> List<T> events(BattleActionLog log, Class<T> type) {
        return log.events().stream()
            .filter(type::isInstance)
            .map(type::cast)
            .toList();
    }

    static <T extends BattleTraceEvent> List<T> traces(BattleActionLog log, Class<T> type) {
        return log.traceEvents().stream()
            .filter(type::isInstance)
            .map(type::cast)
            .toList();
    }

    static boolean usedSkill(BattleActionLog log, UUID personageId, Object skill) {
        return events(log, BattleEvent.SkillWindowUsed.class).stream()
            .anyMatch(event -> event.personageId().equals(personageId) && event.skill().equals(skill));
    }

    static long attemptsBy(BattleActionLog log, BattlePersonage personage) {
        return traces(log, BattleTraceEvent.NormalAttackAttempt.class).stream()
            .filter(attempt -> attempt.attackerId().equals(personage.id()))
            .count();
    }

    record Scenario(
        CombatSimulationTeams teams,
        BattleContext context,
        ControlledRandom random,
        BattleActionLog log
    ) {
    }

    static final class ControlledRandom implements BattleRandom {
        private final SeededBattleRandom delegate;
        private UUID forcedTarget;
        private int dodgeAttempt = -1;
        private int dodgeRolls;

        ControlledRandom(long seed) {
            this.delegate = new SeededBattleRandom(seed);
        }

        void forceTarget(BattlePersonage target) {
            forcedTarget = target.id();
        }

        void forceDodgeOnAttempt(int attempt) {
            if (attempt <= 0) {
                throw new IllegalArgumentException("Dodge attempt must be positive");
            }
            dodgeAttempt = attempt;
            dodgeRolls = 0;
        }

        @Override
        public int nextInt(String sequence, int minimumInclusive, int maximumInclusive) {
            if (sequence.startsWith("normal-critical:")) {
                return maximumInclusive;
            }
            if (sequence.startsWith("normal-dodge:")) {
                dodgeRolls++;
                return dodgeRolls == dodgeAttempt ? minimumInclusive : maximumInclusive;
            }
            if (sequence.startsWith("normal-damage:") || sequence.startsWith("skill-damage-")) {
                return minimumInclusive + (maximumInclusive - minimumInclusive) / 2;
            }
            return delegate.nextInt(sequence, minimumInclusive, maximumInclusive);
        }

        @Override
        public <T> T pickWeighted(String sequence, Map<T, Integer> weights) {
            if (sequence.startsWith("target-selection:") && forcedTarget != null) {
                for (final var candidate : weights.keySet()) {
                    if (candidate instanceof BattlePersonage personage
                        && personage.id().equals(forcedTarget)) {
                        return candidate;
                    }
                }
            }
            return delegate.pickWeighted(sequence, weights);
        }

        @Override
        public <T> List<T> shuffle(String sequence, List<T> values) {
            return delegate.shuffle(sequence, values);
        }
    }

}
