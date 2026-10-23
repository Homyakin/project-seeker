package ru.homyakin.seeker.game.battle;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.battle.Step12MixedBuildMatrix.Benchmark;
import ru.homyakin.seeker.game.battle.Step12MixedBuildMatrix.Comparison;
import ru.homyakin.seeker.game.battle.Step12MixedBuildMatrix.Metric;
import ru.homyakin.seeker.game.battle.Step12MixedBuildMatrix.Rule;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.MixedBuild;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.RoleBuild;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;

class Step12MixedBuildMatrixTest {
    private static final long ROOT_SEED = 2_026_100_101L;

    @Test
    void comparisonsMatchTheExactPreregisteredMap() {
        final var expected = List.of(
            activity("RAM_GUARD", MixedBuild.RAM, RoleBuild.GUARDIAN, Benchmark.GUARD,
                ActiveEnum.GUARD, Metric.GUARD_INTERCEPTIONS_PER_ENEMY_ATTEMPT),
            comparison("RAM_PRESSURE", MixedBuild.RAM, RoleBuild.BRUISER, Benchmark.PRESSURE,
                ActiveEnum.BERSERK, Metric.NORMAL_DAMAGE_PER_TURN, Metric.NORMAL_DAMAGE_PER_TURN),
            activity("KEEPER_GUARD", MixedBuild.KEEPER, RoleBuild.GUARDIAN, Benchmark.GUARD,
                ActiveEnum.GUARD, Metric.GUARD_INTERCEPTIONS_PER_ENEMY_ATTEMPT),
            comparison("KEEPER_TEMPO", MixedBuild.KEEPER, RoleBuild.TACTICIAN_PHYSICAL,
                Benchmark.TEMPO, ActiveEnum.TEMPO_BREAK, Metric.INITIATIVE_REMOVED_PER_TURN,
                Metric.NORMAL_DAMAGE_PER_TURN),
            comparison("DUELIST_PRESSURE", MixedBuild.DUELIST, RoleBuild.BRUISER,
                Benchmark.PRESSURE, ActiveEnum.BERSERK, Metric.NORMAL_DAMAGE_PER_TURN,
                Metric.NORMAL_DAMAGE_PER_TURN),
            activity("DUELIST_MOBILITY", MixedBuild.DUELIST, RoleBuild.SKIRMISHER,
                Benchmark.MOBILITY, ActiveEnum.HIT_AND_RUN, Metric.HIT_AND_RUN_MOVES_PER_TURN),
            activity("RAIDER_MOBILITY", MixedBuild.RAIDER, RoleBuild.SKIRMISHER,
                Benchmark.MOBILITY, ActiveEnum.HIT_AND_RUN, Metric.HIT_AND_RUN_MOVES_PER_TURN),
            comparison("RAIDER_ACCESS", MixedBuild.RAIDER, RoleBuild.ASSASSIN, Benchmark.ACCESS,
                ActiveEnum.PENETRATION, Metric.PENETRATION_DAMAGE_PER_TURN,
                Metric.PENETRATION_DAMAGE_PER_TURN),
            comparison("FORMATION_BREAKER_PRESSURE", MixedBuild.FORMATION_BREAKER, RoleBuild.BRUISER,
                Benchmark.PRESSURE, ActiveEnum.BERSERK, Metric.NORMAL_DAMAGE_PER_TURN,
                Metric.NORMAL_DAMAGE_PER_TURN),
            comparison("FORMATION_BREAKER_TEMPO", MixedBuild.FORMATION_BREAKER,
                RoleBuild.TACTICIAN_PHYSICAL, Benchmark.TEMPO, ActiveEnum.TEMPO_BREAK,
                Metric.INITIATIVE_REMOVED_PER_TURN, Metric.INITIATIVE_REMOVED_PER_TURN),
            price("ARTILLERIST_RANGED", MixedBuild.ARTILLERIST, RoleBuild.RANGER_PHYSICAL,
                Benchmark.RANGED, Metric.RANGED_SUSTAINED_DAMAGE_PER_TURN),
            activity("ARTILLERIST_BURST", MixedBuild.ARTILLERIST, RoleBuild.BREAKER_PHYSICAL,
                Benchmark.BURST, ActiveEnum.ACCUMULATION, Metric.ACCUMULATION_DAMAGE_PER_TURN),
            price("BLOCKER_RANGED", MixedBuild.BLOCKER, RoleBuild.RANGER_PHYSICAL,
                Benchmark.RANGED, Metric.RANGED_SUSTAINED_DAMAGE_PER_TURN),
            new ExpectedComparison(
                "BLOCKER_CONTROL",
                MixedBuild.BLOCKER,
                Optional.of(RoleBuild.TACTICIAN_PHYSICAL),
                Benchmark.CONTROL,
                Optional.of(ActiveEnum.KNOCKBACK),
                Optional.of(Metric.KNOCKBACK_MOVES_PER_TURN),
                Optional.empty(),
                Optional.of(Metric.INITIATIVE_REMOVED_PER_TURN),
                Rule.CONTROL_DIAGNOSTIC
            )
        );
        final var actual = Step12MixedBuildMatrix.comparisons().stream()
            .map(ExpectedComparison::from)
            .toList();

        Assertions.assertEquals(expected, actual);
    }

    @Test
    void cellsCoverAllFourteenComparisonsAndFiveControlLevels() {
        final var cells = Step12MixedBuildMatrix.cells(ROOT_SEED);
        final var levelsByCode = cells.stream().collect(Collectors.groupingBy(
            cell -> cell.comparison().code(),
            Collectors.mapping(Step12MixedBuildMatrix.Cell::level, Collectors.toList())
        ));

        Assertions.assertAll(
            () -> Assertions.assertEquals(70, cells.size()),
            () -> Assertions.assertEquals(14, levelsByCode.size()),
            () -> Assertions.assertTrue(levelsByCode.values().stream().allMatch(
                levels -> levels.equals(Step12SimulationFixtures.CONTROL_LEVELS)
            )),
            () -> Assertions.assertEquals(
                Set.copyOf(Step12MixedBuildMatrix.comparisons()),
                cells.stream().map(Step12MixedBuildMatrix.Cell::comparison)
                    .collect(Collectors.toUnmodifiableSet())
            )
        );
    }

    @Test
    void comparisonCodesCellCodesAndSeedsAreUnique() {
        final var comparisons = Step12MixedBuildMatrix.comparisons();
        final var cells = Step12MixedBuildMatrix.cells(ROOT_SEED);

        Assertions.assertAll(
            () -> Assertions.assertEquals(
                comparisons.size(),
                comparisons.stream().map(Comparison::code).distinct().count()
            ),
            () -> Assertions.assertTrue(comparisons.stream().allMatch(
                comparison -> comparison.code().matches("[A-Z0-9_]+")
            )),
            () -> Assertions.assertEquals(
                cells.size(),
                cells.stream().map(Step12MixedBuildMatrix.Cell::code).distinct().count()
            ),
            () -> Assertions.assertEquals(
                cells.size(),
                cells.stream().map(Step12MixedBuildMatrix.Cell::seed).distinct().count()
            )
        );
    }

    @Test
    void seedsAreDeterministicAndDependOnTheRootAndCellCode() {
        final var first = Step12MixedBuildMatrix.cells(ROOT_SEED);
        final var repeated = Step12MixedBuildMatrix.cells(ROOT_SEED);
        final var anotherRoot = Step12MixedBuildMatrix.cells(ROOT_SEED + 1);

        Assertions.assertAll(
            () -> Assertions.assertEquals(first, repeated),
            () -> Assertions.assertTrue(java.util.stream.IntStream.range(0, first.size()).allMatch(index ->
                first.get(index).seed() != anotherRoot.get(index).seed()
            )),
            () -> Assertions.assertNotEquals(
                Step12MixedBuildMatrix.cellSeed(ROOT_SEED, "RAM_GUARD_L0"),
                Step12MixedBuildMatrix.cellSeed(ROOT_SEED, "RAM_GUARD_L3")
            )
        );
    }

    @Test
    void ruleAndHypothesisCountsMatchThePreregisteredDecisionModel() {
        final Map<Rule, Long> rules = Step12MixedBuildMatrix.comparisons().stream()
            .map(Comparison::rule)
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        final long prices = Step12MixedBuildMatrix.comparisons().stream()
            .filter(comparison -> comparison.priceMetric().isPresent())
            .count() * Step12SimulationFixtures.CONTROL_LEVELS.size();
        final long activities = Step12MixedBuildMatrix.comparisons().stream()
            .filter(comparison -> comparison.activityMetric().isPresent())
            .count() * Step12SimulationFixtures.CONTROL_LEVELS.size();

        Assertions.assertAll(
            () -> Assertions.assertEquals(
                Map.of(
                    Rule.LOWER_THAN_REFERENCE, 8L,
                    Rule.MECHANICS_PRESENT, 5L,
                    Rule.CONTROL_DIAGNOSTIC, 1L
                ),
                rules
            ),
            () -> Assertions.assertEquals(40, prices),
            () -> Assertions.assertEquals(60, activities),
            () -> Assertions.assertEquals(105, prices + activities + 5)
        );
    }

    @Test
    void exactGuardianPricesHoldAtEveryControlLevel() {
        for (final int level : Step12SimulationFixtures.CONTROL_LEVELS) {
            final var guardian = Step12SimulationFixtures.personage(RoleBuild.GUARDIAN, level);
            final var ram = Step12SimulationFixtures.personage(MixedBuild.RAM, level);
            final var keeper = Step12SimulationFixtures.personage(MixedBuild.KEEPER, level);

            Assertions.assertAll(
                "level " + level,
                () -> Assertions.assertTrue(ram.maxHealth() < guardian.maxHealth()),
                () -> Assertions.assertTrue(
                    ram.defenses().values().stream().mapToInt(Integer::intValue).sum()
                        < guardian.defenses().values().stream().mapToInt(Integer::intValue).sum()
                ),
                () -> Assertions.assertTrue(
                    points(ram, ActiveEnum.GUARD) < points(guardian, ActiveEnum.GUARD)
                ),
                () -> Assertions.assertTrue(keeper.maxHealth() < guardian.maxHealth()),
                () -> Assertions.assertTrue(keeper.totalThreat() < guardian.totalThreat()),
                () -> Assertions.assertTrue(
                    points(keeper, ActiveEnum.GUARD) < points(guardian, ActiveEnum.GUARD)
                )
            );
        }
    }

    private static ExpectedComparison comparison(
        String code,
        MixedBuild mixed,
        RoleBuild reference,
        Benchmark benchmark,
        ActiveEnum skill,
        Metric activity,
        Metric price
    ) {
        return new ExpectedComparison(
            code, mixed, Optional.of(reference), benchmark,
            Optional.of(skill), Optional.of(activity), Optional.of(price), Optional.of(price),
            Rule.LOWER_THAN_REFERENCE
        );
    }

    private static ExpectedComparison activity(
        String code,
        MixedBuild mixed,
        RoleBuild reference,
        Benchmark benchmark,
        ActiveEnum skill,
        Metric metric
    ) {
        return new ExpectedComparison(
            code, mixed, Optional.of(reference), benchmark,
            Optional.of(skill), Optional.of(metric), Optional.empty(), Optional.empty(),
            Rule.MECHANICS_PRESENT
        );
    }

    private static ExpectedComparison price(
        String code,
        MixedBuild mixed,
        RoleBuild reference,
        Benchmark benchmark,
        Metric metric
    ) {
        return new ExpectedComparison(
            code, mixed, Optional.of(reference), benchmark,
            Optional.empty(), Optional.empty(), Optional.of(metric), Optional.of(metric),
            Rule.LOWER_THAN_REFERENCE
        );
    }

    private static int points(BattlePersonage personage, ActiveEnum skill) {
        return personage.skillSnapshots().stream()
            .filter(snapshot -> snapshot.code() == skill)
            .mapToInt(BattleSkillInitSnapshot::points)
            .findFirst()
            .orElse(0);
    }

    private record ExpectedComparison(
        String code,
        MixedBuild mixedBuild,
        Optional<RoleBuild> referenceBuild,
        Benchmark benchmark,
        Optional<ActiveEnum> activitySkill,
        Optional<Metric> activityMetric,
        Optional<Metric> priceMetric,
        Optional<Metric> referenceMetric,
        Rule rule
    ) {
        private static ExpectedComparison from(Comparison comparison) {
            return new ExpectedComparison(
                comparison.code(), comparison.mixedBuild(), comparison.referenceBuild(),
                comparison.benchmark(), comparison.activitySkill(), comparison.activityMetric(),
                comparison.priceMetric(), comparison.referenceMetric(), comparison.rule()
            );
        }
    }
}
