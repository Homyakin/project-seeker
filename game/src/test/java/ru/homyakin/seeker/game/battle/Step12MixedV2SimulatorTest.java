package ru.homyakin.seeker.game.battle;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.battle.Step12MixedBuildMatrix.Cell;
import ru.homyakin.seeker.game.battle.Step12MixedBuildMatrix.Rule;
import ru.homyakin.seeker.game.battle.Step12MixedV2Simulator.CellResult;
import ru.homyakin.seeker.game.battle.Step12MixedV2Simulator.Configuration;
import ru.homyakin.seeker.game.battle.Step12MixedV2Simulator.Gate;
import ru.homyakin.seeker.game.battle.Step12MixedBuildStatistics.Estimate;
import ru.homyakin.seeker.game.battle.Step12MixedBuildStatistics.Hypothesis;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.RoleBuild;

class Step12MixedV2SimulatorTest {
    @Test
    void runCellsIsReproducibleWithOneAndTwoWorkers() {
        final var cells = Step12MixedBuildMatrix.cells(Step12MixedV2Simulator.DIAGNOSTIC_ROOT)
            .stream()
            .filter(cell -> cell.level() == 0)
            .filter(cell -> SetOfCodes.contains(cell.comparison().code()))
            .toList();

        final var sequential = Step12MixedV2Simulator.runCells(cells, 2, 10_000, 1);
        final var parallel = Step12MixedV2Simulator.runCells(cells, 2, 10_000, 2);

        Assertions.assertAll(
            () -> Assertions.assertEquals(2, cells.size()),
            () -> Assertions.assertEquals(sequential, parallel),
            () -> Assertions.assertEquals(
                cells.stream().map(Cell::code).toList(),
                parallel.stream().map(result -> result.cell().code()).toList()
            )
        );
    }

    @Test
    void finalConfigurationAcceptsOnlyTheFrozenExecutionContract() {
        final var valid = finalConfiguration(
            Step12MixedV2Simulator.FINAL_ROOT,
            Step12MixedV2Simulator.FINAL_ITERATIONS,
            Step12MixedV2Simulator.REQUIRED_MAX_ROUNDS,
            4,
            true,
            "",
            Step12MixedV2Simulator.FINAL_OUTPUT
        );

        Assertions.assertAll(
            () -> Assertions.assertDoesNotThrow(() ->
                Step12MixedV2Simulator.validateConfiguration(valid)
            ),
            () -> assertInvalidFinal(finalConfiguration(
                Step12MixedV2Simulator.DIAGNOSTIC_ROOT,
                Step12MixedV2Simulator.FINAL_ITERATIONS,
                Step12MixedV2Simulator.REQUIRED_MAX_ROUNDS,
                4,
                true,
                "",
                Step12MixedV2Simulator.FINAL_OUTPUT
            )),
            () -> assertInvalidFinal(finalConfiguration(
                Step12MixedV2Simulator.FINAL_ROOT,
                Step12MixedV2Simulator.FINAL_ITERATIONS - 1,
                Step12MixedV2Simulator.REQUIRED_MAX_ROUNDS,
                4,
                true,
                "",
                Step12MixedV2Simulator.FINAL_OUTPUT
            )),
            () -> assertInvalidFinal(finalConfiguration(
                Step12MixedV2Simulator.FINAL_ROOT,
                Step12MixedV2Simulator.FINAL_ITERATIONS,
                Step12MixedV2Simulator.REQUIRED_MAX_ROUNDS - 1,
                4,
                true,
                "",
                Step12MixedV2Simulator.FINAL_OUTPUT
            )),
            () -> assertInvalidFinal(finalConfiguration(
                Step12MixedV2Simulator.FINAL_ROOT,
                Step12MixedV2Simulator.FINAL_ITERATIONS,
                Step12MixedV2Simulator.REQUIRED_MAX_ROUNDS,
                2,
                true,
                "",
                Step12MixedV2Simulator.FINAL_OUTPUT
            )),
            () -> assertInvalidFinal(finalConfiguration(
                Step12MixedV2Simulator.FINAL_ROOT,
                Step12MixedV2Simulator.FINAL_ITERATIONS,
                Step12MixedV2Simulator.REQUIRED_MAX_ROUNDS,
                4,
                false,
                "",
                Step12MixedV2Simulator.FINAL_OUTPUT
            )),
            () -> assertInvalidFinal(finalConfiguration(
                Step12MixedV2Simulator.FINAL_ROOT,
                Step12MixedV2Simulator.FINAL_ITERATIONS,
                Step12MixedV2Simulator.REQUIRED_MAX_ROUNDS,
                4,
                true,
                "BLOCKER_CONTROL_L0",
                Step12MixedV2Simulator.FINAL_OUTPUT
            )),
            () -> assertInvalidFinal(finalConfiguration(
                Step12MixedV2Simulator.FINAL_ROOT,
                Step12MixedV2Simulator.FINAL_ITERATIONS,
                Step12MixedV2Simulator.REQUIRED_MAX_ROUNDS,
                4,
                true,
                "",
                "target/not-the-official-report.md"
            )),
            () -> assertInvalidFinal(withStatistics(valid, "target/not-the-official-statistics.tsv"))
        );
    }

    @Test
    void diagnosticConfigurationCannotExceedTheDeclaredRoundLimit() {
        final var configuration = new Configuration(
            "DIAGNOSTIC",
            Step12MixedV2Simulator.DIAGNOSTIC_ROOT,
            100,
            Step12MixedV2Simulator.REQUIRED_MAX_ROUNDS + 1,
            1,
            false,
            "",
            "target/diagnostic.md",
            "target/diagnostic.tsv"
        );

        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> Step12MixedV2Simulator.validateConfiguration(configuration)
        );
    }

    @Test
    void exactStatisticsKeepSufficientValuesWithoutDecimalRounding() {
        final var cell = Step12MixedBuildMatrix.cells(Step12MixedV2Simulator.FINAL_ROOT).getFirst();
        final var estimate = Step12MixedBuildStatistics.estimate(new double[] {0.1, 0.2, 0.4});
        final var code = cell.code() + "_ACTIVITY";
        final var hypothesis = new Hypothesis(code, estimate);
        final var gate = new Gate(code, 0.75, 0.5, estimate);
        final var result = new CellResult(
            cell,
            Optional.of(gate),
            Optional.empty(),
            Optional.empty(),
            List.of(hypothesis),
            0,
            Optional.empty(),
            0,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            true
        );

        final var statistics = Step12MixedV2Simulator.statisticsTsv(
            List.of(result),
            Step12MixedBuildStatistics.holm(List.of(hypothesis))
        );

        Assertions.assertAll(
            () -> Assertions.assertEquals(2, statistics.lines().count()),
            () -> Assertions.assertTrue(statistics.contains("\tsampleSize\t")),
            () -> Assertions.assertTrue(statistics.contains("\t3\t")),
            () -> Assertions.assertTrue(statistics.contains(Double.toHexString(estimate.point()))),
            () -> Assertions.assertTrue(statistics.contains(
                Double.toHexString(estimate.sumSquaredDeviations())
            ))
        );
    }

    @Test
    void completeMatrixHasSeventyCellsAndExactlyOneHundredAndFiveHypotheses() {
        final var results = completeSyntheticResults();
        final var adjusted = Step12MixedV2Simulator.adjust(results, true);
        final var blocker = Step12MixedBuildMatrix.comparisons().stream()
            .filter(comparison -> comparison.code().equals("BLOCKER_CONTROL"))
            .findFirst()
            .orElseThrow();

        Assertions.assertAll(
            () -> Assertions.assertEquals(Step12MixedV2Simulator.EXPECTED_CELLS, results.size()),
            () -> Assertions.assertEquals(
                Step12MixedV2Simulator.EXPECTED_HYPOTHESES,
                results.stream().mapToInt(result -> result.hypotheses().size()).sum()
            ),
            () -> Assertions.assertEquals(Step12MixedV2Simulator.EXPECTED_HYPOTHESES,
                adjusted.size()),
            () -> Assertions.assertEquals(Optional.of(RoleBuild.TACTICIAN_PHYSICAL),
                blocker.referenceBuild()),
            () -> Assertions.assertTrue(blocker.priceMetric().isEmpty()),
            () -> Assertions.assertEquals(2, hypothesisCount(blocker))
        );
    }

    @Test
    void finalAdjustmentRejectsIncompleteCellAndHypothesisSets() {
        final var complete = completeSyntheticResults();
        final var missingCell = complete.subList(0, complete.size() - 1);
        final var missingHypothesis = new ArrayList<>(complete);
        final var last = missingHypothesis.getLast();
        missingHypothesis.set(
            missingHypothesis.size() - 1,
            result(last.cell(), last.hypotheses().subList(0, last.hypotheses().size() - 1))
        );

        Assertions.assertAll(
            () -> Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> Step12MixedV2Simulator.adjust(missingCell, true)
            ),
            () -> Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> Step12MixedV2Simulator.adjust(missingHypothesis, true)
            )
        );
    }

    private static List<CellResult> completeSyntheticResults() {
        return Step12MixedBuildMatrix.cells(Step12MixedV2Simulator.FINAL_ROOT).stream()
            .map(cell -> {
                final var hypotheses = new ArrayList<Hypothesis>();
                if (cell.comparison().activityMetric().isPresent()) {
                    hypotheses.add(hypothesis(cell.code() + "_ACTIVITY"));
                }
                if (cell.comparison().priceMetric().isPresent()) {
                    hypotheses.add(hypothesis(cell.code() + "_PRICE"));
                }
                if (cell.comparison().rule() == Rule.CONTROL_DIAGNOSTIC) {
                    hypotheses.add(hypothesis(cell.code() + "_CONTROL_LOSS"));
                }
                return result(cell, hypotheses);
            })
            .toList();
    }

    private static int hypothesisCount(Step12MixedBuildMatrix.Comparison comparison) {
        int count = comparison.activityMetric().isPresent() ? 1 : 0;
        count += comparison.priceMetric().isPresent() ? 1 : 0;
        count += comparison.rule() == Rule.CONTROL_DIAGNOSTIC ? 1 : 0;
        return count;
    }

    private static Hypothesis hypothesis(String code) {
        return new Hypothesis(code, new Estimate(1, 1, 1, 0, 0, 2, 0));
    }

    private static CellResult result(Cell cell, List<Hypothesis> hypotheses) {
        return new CellResult(
            cell,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            hypotheses,
            0,
            Optional.empty(),
            0,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            true
        );
    }

    private static Configuration finalConfiguration(
        long rootSeed,
        int iterations,
        int maxRounds,
        int workers,
        boolean enforce,
        String scenario,
        String output
    ) {
        return new Configuration(
            "FINAL",
            rootSeed,
            iterations,
            maxRounds,
            workers,
            enforce,
            scenario,
            output,
            Step12MixedV2Simulator.FINAL_STATISTICS_OUTPUT
        );
    }

    private static Configuration withStatistics(Configuration configuration, String statistics) {
        return new Configuration(
            configuration.mode(),
            configuration.rootSeed(),
            configuration.iterations(),
            configuration.maxRounds(),
            configuration.workers(),
            configuration.enforce(),
            configuration.scenario(),
            configuration.output(),
            statistics
        );
    }

    private static void assertInvalidFinal(Configuration configuration) {
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> Step12MixedV2Simulator.validateConfiguration(configuration)
        );
    }

    private static final class SetOfCodes {
        private static final List<String> VALUES = List.of("ARTILLERIST_RANGED", "BLOCKER_RANGED");

        private SetOfCodes() {
        }

        private static boolean contains(String code) {
            return VALUES.contains(code);
        }
    }
}
