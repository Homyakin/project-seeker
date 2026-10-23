package ru.homyakin.seeker.game.battle;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import ru.homyakin.seeker.game.battle.Step12MixedBuildMatrix.Cell;
import ru.homyakin.seeker.game.battle.Step12MixedBuildMatrix.Metric;
import ru.homyakin.seeker.game.battle.Step12MixedBuildObservationRunner.Observation;
import ru.homyakin.seeker.game.battle.Step12MixedBuildStatistics.AdjustedHypothesis;
import ru.homyakin.seeker.game.battle.Step12MixedBuildStatistics.Estimate;
import ru.homyakin.seeker.game.battle.Step12MixedBuildStatistics.Hypothesis;

/** Запускаемый по явному свойству исполнитель слоя смешанных сборок шага 12. */
@EnabledIfSystemProperty(named = "step12.mixed.enabled", matches = "true")
class Step12MixedBuildSimulator {
    static final long DIAGNOSTIC_ROOT = 2_026_092_501L;
    static final long FINAL_ROOT = 2_026_092_601L;
    static final int FINAL_ITERATIONS = 10_000;
    static final int REQUIRED_MAX_ROUNDS = 10_000;
    static final int EXPECTED_CELLS = 70;
    static final int EXPECTED_HYPOTHESES = 105;
    static final String FINAL_OUTPUT = "../"
        + Step12MixedV1AcceptanceLedger.FINAL_REPORT_RELATIVE_PATH;
    static final String FINAL_STATISTICS_OUTPUT = "../"
        + Step12MixedV1AcceptanceLedger.FINAL_STATISTICS_RELATIVE_PATH;

    private static final String PREFIX = "step12.mixed.";

    @Test
    void runConfiguredMatrix() throws Exception {
        final var configuration = configuration();
        validateConfiguration(configuration);
        Step12MixedV1AcceptanceLedger ledger = null;
        Step12MixedV1AcceptanceLedger.FinalAttempt finalAttempt = null;
        if (configuration.mode().equals("FINAL")) {
            ledger = Step12MixedV1AcceptanceLedger.fromTestClasses(
                Step12MixedBuildSimulator.class
            );
            finalAttempt = ledger.claimFinalExecution(
                configuration.rootSeed(),
                configuration.iterations(),
                configuration.maxRounds(),
                configuration.workers(),
                Step12MixedV1AcceptanceLedger.expectedFinalCellDescriptors(),
                Step12MixedV1AcceptanceLedger.expectedFinalHypothesisDescriptors(),
                Path.of(configuration.output()).toAbsolutePath().normalize().toString(),
                Path.of(configuration.statistics()).toAbsolutePath().normalize().toString()
            );
        }
        final var selectedCells = selectCells(configuration);
        final var rawResults = runCells(
            selectedCells,
            configuration.iterations(),
            configuration.maxRounds(),
            configuration.workers()
        );
        final var adjusted = adjust(rawResults, configuration.mode().equals("FINAL"));
        final var statistics = statisticsTsv(rawResults, adjusted);
        final var statisticsOutput = Path.of(configuration.statistics());
        final var output = Path.of(configuration.output());
        if (statisticsOutput.getParent() != null) {
            Files.createDirectories(statisticsOutput.getParent());
        }
        if (output.getParent() != null) {
            Files.createDirectories(output.getParent());
        }
        if (configuration.mode().equals("FINAL")) {
            ledger.writeFinalStatistics(finalAttempt, statistics);
        } else {
            Files.writeString(statisticsOutput, statistics);
        }
        final var statisticsSha256 = Step12MixedV1InputManifest.fileSha256(statisticsOutput);
        final var markdown = markdown(configuration, rawResults, adjusted, statisticsSha256);
        if (configuration.mode().equals("FINAL")) {
            ledger.writeFinalReport(finalAttempt, markdown);
        } else {
            Files.writeString(output, markdown);
        }
        System.out.println(markdown);
        System.out.println("Отчёт: " + output.toAbsolutePath());
        System.out.println("Точные статистики: " + statisticsOutput.toAbsolutePath());
        System.out.println("SHA-256 точных статистик: " + statisticsSha256);

        if (configuration.enforce()) {
            final var failures = failures(rawResults, adjusted);
            Assertions.assertTrue(failures.isEmpty(), () -> String.join(System.lineSeparator(), failures));
        }
    }

    static Configuration configuration() {
        final var mode = property("mode", "DIAGNOSTIC").toUpperCase(Locale.ROOT);
        final long defaultSeed = mode.equals("FINAL") ? FINAL_ROOT : DIAGNOSTIC_ROOT;
        final int defaultIterations = mode.equals("FINAL") ? FINAL_ITERATIONS : 100;
        final var defaultOutput = mode.equals("FINAL")
            ? FINAL_OUTPUT
            : "target/step12-mixed-diagnostic.md";
        final var defaultStatistics = mode.equals("FINAL")
            ? FINAL_STATISTICS_OUTPUT
            : "target/step12-mixed-diagnostic-statistics.tsv";
        return new Configuration(
            mode,
            longProperty("seed", defaultSeed),
            intProperty("iterations", defaultIterations),
            intProperty("maxRounds", REQUIRED_MAX_ROUNDS),
            intProperty("workers", Math.min(4, Runtime.getRuntime().availableProcessors())),
            booleanProperty("enforce", false),
            property("scenario", "").trim(),
            property("output", defaultOutput),
            property("statistics", defaultStatistics)
        );
    }

    static void validateConfiguration(Configuration configuration) {
        if (!Set.of("DIAGNOSTIC", "FINAL").contains(configuration.mode())) {
            throw new IllegalArgumentException("Mixed-build mode must be DIAGNOSTIC or FINAL");
        }
        if (configuration.iterations() < 2 || configuration.maxRounds() <= 0 || configuration.workers() <= 0) {
            throw new IllegalArgumentException("Iterations, maximum rounds and workers must be positive");
        }
        if (configuration.maxRounds() > REQUIRED_MAX_ROUNDS) {
            throw new IllegalArgumentException("Maximum rounds must not exceed " + REQUIRED_MAX_ROUNDS);
        }
        if (configuration.output().isBlank() || configuration.statistics().isBlank()) {
            throw new IllegalArgumentException("Output paths must not be blank");
        }
        if (configuration.mode().equals("DIAGNOSTIC") && configuration.rootSeed() != DIAGNOSTIC_ROOT) {
            throw new IllegalArgumentException("Diagnostic root must equal " + DIAGNOSTIC_ROOT);
        }
        if (configuration.mode().equals("FINAL")) {
            final var violations = new ArrayList<String>();
            if (configuration.rootSeed() != FINAL_ROOT) {
                violations.add("root seed must equal " + FINAL_ROOT);
            }
            if (configuration.iterations() != FINAL_ITERATIONS) {
                violations.add("iterations must equal " + FINAL_ITERATIONS);
            }
            if (configuration.maxRounds() != REQUIRED_MAX_ROUNDS) {
                violations.add("maxRounds must equal " + REQUIRED_MAX_ROUNDS);
            }
            if (configuration.workers() != 4) {
                violations.add("workers must equal 4");
            }
            if (!configuration.enforce()) {
                violations.add("enforce must be true");
            }
            if (!configuration.scenario().isEmpty()) {
                violations.add("scenario filter must be empty");
            }
            if (!configuration.output().equals(FINAL_OUTPUT)) {
                violations.add("output must equal " + FINAL_OUTPUT);
            }
            if (!configuration.statistics().equals(FINAL_STATISTICS_OUTPUT)) {
                violations.add("statistics must equal " + FINAL_STATISTICS_OUTPUT);
            }
            if (!violations.isEmpty()) {
                throw new IllegalArgumentException(
                    "Invalid mixed-build final configuration: " + String.join("; ", violations)
                );
            }
        }
    }

    static List<CellResult> runCells(List<Cell> cells, int iterations, int maxRounds, int workers) {
        if (cells.isEmpty()) {
            throw new IllegalArgumentException("At least one mixed-build cell is required");
        }
        if (iterations < 2 || maxRounds <= 0 || workers <= 0) {
            throw new IllegalArgumentException("Invalid mixed-build execution parameters");
        }
        if (workers == 1 || cells.size() == 1) {
            return cells.stream().map(cell -> runCell(cell, iterations, maxRounds)).toList();
        }
        final var pool = new ForkJoinPool(Math.min(workers, cells.size()));
        try {
            return pool.submit(() -> cells.parallelStream()
                .map(cell -> runCell(cell, iterations, maxRounds))
                .toList()
            ).get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Mixed-build execution was interrupted", exception);
        } catch (ExecutionException exception) {
            final var cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException("Mixed-build execution failed", cause);
        } finally {
            pool.shutdown();
        }
    }

    static List<AdjustedHypothesis> adjust(List<CellResult> results, boolean requireCompleteSet) {
        final var hypotheses = results.stream().flatMap(result -> result.hypotheses().stream()).toList();
        if (requireCompleteSet) {
            if (results.size() != EXPECTED_CELLS) {
                throw new IllegalArgumentException("Final mixed-build run requires exactly 70 cells");
            }
            if (hypotheses.size() != EXPECTED_HYPOTHESES) {
                throw new IllegalArgumentException("Final mixed-build run requires exactly 105 hypotheses");
            }
        }
        return Step12MixedBuildStatistics.holm(hypotheses);
    }

    private static List<Cell> selectCells(Configuration configuration) {
        final var cells = Step12MixedBuildMatrix.cells(configuration.rootSeed());
        if (configuration.scenario().isEmpty()) {
            return cells;
        }
        if (configuration.mode().equals("FINAL")) {
            throw new IllegalArgumentException("Final mixed-build run cannot be filtered");
        }
        final var selected = cells.stream()
            .filter(cell -> cell.code().equalsIgnoreCase(configuration.scenario()))
            .toList();
        if (selected.isEmpty()) {
            throw new IllegalArgumentException("Unknown mixed-build scenario: " + configuration.scenario());
        }
        return selected;
    }

    private static CellResult runCell(Cell cell, int iterations, int maxRounds) {
        final var comparison = cell.comparison();
        final var mixedActivity = optionalArray(comparison.activityMetric(), iterations);
        final var disabledActivity = optionalArray(comparison.activityMetric(), iterations);
        final var mixedPrice = optionalArray(comparison.priceMetric(), iterations);
        final var referencePrice = optionalArray(comparison.priceMetric(), iterations);
        final boolean rangedPrice = comparison.priceMetric()
            .filter(metric -> metric == Metric.RANGED_SUSTAINED_DAMAGE_PER_TURN)
            .isPresent();
        final var mixedFarAttemptShares = rangedPrice ? new double[iterations] : null;
        final var referenceFarAttemptShares = rangedPrice ? new double[iterations] : null;
        final var controlActive = comparison.rule() == Step12MixedBuildMatrix.Rule.CONTROL_DIAGNOSTIC
            ? new double[iterations]
            : null;
        final var controlDisabled = comparison.rule() == Step12MixedBuildMatrix.Rule.CONTROL_DIAGNOSTIC
            ? new double[iterations]
            : null;
        final var controlReference = comparison.rule() == Step12MixedBuildMatrix.Rule.CONTROL_DIAGNOSTIC
            ? new double[iterations]
            : null;
        final var mixedWins = new double[iterations];
        final var referenceWins = comparison.referenceBuild().isPresent() ? new double[iterations] : null;
        final var firstSideWins = new double[iterations];
        var knockbackLimitKept = true;

        for (int iteration = 0; iteration < iterations; iteration++) {
            final Observation observation = Step12MixedBuildObservationRunner.observe(
                cell,
                iteration,
                maxRounds
            );
            knockbackLimitKept &= observation.mixedOn().knockbackLimitKept();
            mixedWins[iteration] = observation.mixedOn().evaluatedWinRate();
            var firstSideSum = observation.mixedOn().firstSideWinRate();
            var firstSideWorlds = 1;
            if (comparison.activityMetric().isPresent()) {
                final Metric metric = comparison.activityMetric().orElseThrow();
                requireDenominator(observation.mixedOn(), metric, cell, "mixed-on");
                final var disabled = observation.mixedOff().orElseThrow();
                requireDenominator(disabled, metric, cell, "mixed-off");
                mixedActivity[iteration] = observation.mixedOn().metric(metric);
                disabledActivity[iteration] = disabled.metric(metric);
                firstSideSum += disabled.firstSideWinRate();
                firstSideWorlds++;
            }
            if (comparison.priceMetric().isPresent()) {
                final Metric mixedMetric = comparison.priceMetric().orElseThrow();
                final Metric referenceMetric = comparison.referenceMetric().orElseThrow();
                final var reference = observation.reference().orElseThrow();
                requireDenominator(observation.mixedOn(), mixedMetric, cell, "mixed-on price");
                requireDenominator(reference, referenceMetric, cell, "reference price");
                mixedPrice[iteration] = observation.mixedOn().metric(mixedMetric);
                referencePrice[iteration] = reference.metric(referenceMetric);
                if (rangedPrice) {
                    mixedFarAttemptShares[iteration] = observation.mixedOn().farNormalAttemptShare();
                    referenceFarAttemptShares[iteration] = reference.farNormalAttemptShare();
                }
            }
            if (observation.reference().isPresent()) {
                final var reference = observation.reference().orElseThrow();
                referenceWins[iteration] = reference.evaluatedWinRate();
                firstSideSum += reference.firstSideWinRate();
                firstSideWorlds++;
            }
            if (controlActive != null) {
                final var disabled = observation.mixedOff().orElseThrow();
                final var reference = observation.reference().orElseThrow();
                final var referenceMetric = comparison.referenceMetric().orElseThrow();
                if (observation.mixedOn().opponentTurns() == 0 || disabled.opponentTurns() == 0) {
                    throw new IllegalStateException(cell.code() + " produced no opponent turns");
                }
                requireDenominator(reference, referenceMetric, cell, "control reference");
                controlActive[iteration] = observation.mixedOn().opponentMovementOnlyTurnShare();
                controlDisabled[iteration] = disabled.opponentMovementOnlyTurnShare();
                controlReference[iteration] = reference.metric(referenceMetric);
            }
            firstSideWins[iteration] = firstSideSum / firstSideWorlds;
        }

        final var hypotheses = new ArrayList<Hypothesis>(3);
        final Optional<Gate> activity;
        if (mixedActivity != null) {
            final var estimate = Step12MixedBuildStatistics.pairedDifference(
                mixedActivity,
                disabledActivity
            );
            final var code = cell.code() + "_ACTIVITY";
            hypotheses.add(new Hypothesis(code, estimate));
            activity = Optional.of(new Gate(code, mean(mixedActivity), mean(disabledActivity), estimate));
        } else {
            activity = Optional.empty();
        }
        final Optional<Gate> price;
        if (mixedPrice != null) {
            final var estimate = Step12MixedBuildStatistics.pairedDifference(
                referencePrice,
                mixedPrice
            );
            final var code = cell.code() + "_PRICE";
            hypotheses.add(new Hypothesis(code, estimate));
            price = Optional.of(new Gate(code, mean(referencePrice), mean(mixedPrice), estimate));
        } else {
            price = Optional.empty();
        }
        final Optional<Gate> control;
        if (controlActive != null) {
            final var estimate = Step12MixedBuildStatistics.pairedDifference(
                controlActive,
                controlDisabled
            );
            final var code = cell.code() + "_CONTROL_LOSS";
            hypotheses.add(new Hypothesis(code, estimate));
            control = Optional.of(new Gate(code, mean(controlActive), mean(controlDisabled), estimate));
        } else {
            control = Optional.empty();
        }
        return new CellResult(
            cell,
            activity,
            price,
            control,
            List.copyOf(hypotheses),
            mean(mixedWins),
            referenceWins == null ? Optional.empty() : Optional.of(mean(referenceWins)),
            mean(firstSideWins),
            rangedPrice ? Optional.of(mean(mixedFarAttemptShares)) : Optional.empty(),
            rangedPrice ? Optional.of(mean(referenceFarAttemptShares)) : Optional.empty(),
            controlReference == null ? Optional.empty() : Optional.of(mean(controlReference)),
            knockbackLimitKept
        );
    }

    private static double[] optionalArray(Optional<?> present, int size) {
        return present.isPresent() ? new double[size] : null;
    }

    private static void requireDenominator(
        Step12MixedBuildObservationRunner.PairMeasure measure,
        Metric metric,
        Cell cell,
        String world
    ) {
        if (!measure.hasDenominator(metric)) {
            throw new IllegalStateException(cell.code() + " has a zero denominator in " + world);
        }
    }

    private static List<String> failures(
        List<CellResult> results,
        List<AdjustedHypothesis> adjusted
    ) {
        final var adjustedByCode = adjusted.stream().collect(java.util.stream.Collectors.toMap(
            AdjustedHypothesis::code,
            hypothesis -> hypothesis
        ));
        final var failures = new ArrayList<String>();
        for (final var result : results) {
            if (!result.knockbackLimitKept()) {
                failures.add(result.cell().code() + ": нарушен предел одного отбрасывания на событие");
            }
            for (final var hypothesis : result.hypotheses()) {
                final var decision = adjustedByCode.get(hypothesis.code());
                if (decision == null || !decision.accepted()) {
                    failures.add(result.cell().code() + ": не принят знак " + hypothesis.code());
                }
            }
        }
        return List.copyOf(failures);
    }

    static String statisticsTsv(
        List<CellResult> results,
        List<AdjustedHypothesis> adjusted
    ) {
        final var adjustedByCode = new HashMap<String, AdjustedHypothesis>();
        adjusted.forEach(hypothesis -> adjustedByCode.put(hypothesis.code(), hypothesis));
        final var builder = new StringBuilder()
            .append("hypothesis\tcell\tlevel\tcondition\tsampleSize\tfirstMean\tsecondMean\t")
            .append("differenceMean\tsumSquaredDeviations\tstandardError\tlower95\tupper95\t")
            .append("pValue\tholmAdjustedPValue\taccepted\n");
        for (final var result : results) {
            result.activity().ifPresent(gate -> appendStatistics(
                builder, result.cell(), "ACTIVITY", gate, adjustedByCode
            ));
            result.price().ifPresent(gate -> appendStatistics(
                builder, result.cell(), "PRICE", gate, adjustedByCode
            ));
            result.control().ifPresent(gate -> appendStatistics(
                builder, result.cell(), "CONTROL_LOSS", gate, adjustedByCode
            ));
        }
        final var statistics = builder.toString();
        if (statistics.indexOf('\r') >= 0) {
            throw new IllegalStateException("Statistics output must use canonical line endings");
        }
        if (statistics.lines().count() != adjusted.size() + 1L) {
            throw new IllegalStateException("Every adjusted hypothesis must have one statistics row");
        }
        return statistics;
    }

    private static void appendStatistics(
        StringBuilder builder,
        Cell cell,
        String condition,
        Gate gate,
        Map<String, AdjustedHypothesis> adjustedByCode
    ) {
        final var adjusted = adjustedByCode.get(gate.code());
        if (adjusted == null || !adjusted.estimate().equals(gate.estimate())) {
            throw new IllegalStateException("Missing or inconsistent adjusted hypothesis: " + gate.code());
        }
        final var estimate = gate.estimate();
        builder.append(gate.code()).append('\t')
            .append(cell.code()).append('\t')
            .append(cell.level()).append('\t')
            .append(condition).append('\t')
            .append(estimate.sampleSize()).append('\t')
            .append(exact(gate.first())).append('\t')
            .append(exact(gate.second())).append('\t')
            .append(exact(estimate.point())).append('\t')
            .append(exact(estimate.sumSquaredDeviations())).append('\t')
            .append(exact(estimate.standardError())).append('\t')
            .append(exact(estimate.lower())).append('\t')
            .append(exact(estimate.upper())).append('\t')
            .append(exact(estimate.pValue())).append('\t')
            .append(exact(adjusted.adjustedPValue())).append('\t')
            .append(adjusted.accepted()).append('\n');
    }

    private static String markdown(
        Configuration configuration,
        List<CellResult> results,
        List<AdjustedHypothesis> adjusted,
        String statisticsSha256
    ) {
        final var adjustedByCode = new HashMap<String, AdjustedHypothesis>();
        adjusted.forEach(hypothesis -> adjustedByCode.put(hypothesis.code(), hypothesis));
        final var failures = failures(results, adjusted);
        final boolean finalMode = configuration.mode().equals("FINAL");
        final var decision = finalMode
            ? (failures.isEmpty() ? "принято" : "не принято")
            : "проверка проводки; решение о балансе не выносится";
        final var builder = new StringBuilder()
            .append("# Шаг 12. Результат проверки смешанных сборок\n\n")
            .append("Режим: `").append(configuration.mode()).append("`; корень: `")
            .append(configuration.rootSeed()).append("`; начальных значений на ячейку: ")
            .append(configuration.iterations()).append("; предел раундов: ")
            .append(configuration.maxRounds()).append(".\n\n")
            .append("Проверено ячеек: ").append(results.size())
            .append("; знаковых условий: ").append(adjusted.size())
            .append("; итог: **").append(decision).append("**.\n\n")
            .append("Точные достаточные статистики: `")
            .append(configuration.statistics()).append("`; SHA-256: `")
            .append(statisticsSha256).append("`.\n\n")
            .append("| Ячейка | Уровень | Условие | Первое значение | Второе значение | ")
            .append("Разность, 95% ДИ | Холм p | Итог |\n")
            .append("|---|---:|---|---:|---:|---:|---:|---|\n");
        for (final var result : results) {
            result.activity().ifPresent(gate -> appendGate(
                builder, result.cell(), "работа механики: включено − выключено", gate, adjustedByCode
            ));
            result.price().ifPresent(gate -> appendGate(
                builder, result.cell(), "цена: чистая роль − смешанная сборка", gate, adjustedByCode
            ));
            result.control().ifPresent(gate -> appendGate(
                builder, result.cell(), "потеря попыток: включено − выключено", gate, adjustedByCode
            ));
        }
        builder.append("\n| Ячейка | Победы смешанной | Победы чистой | Победы первого расположения | ")
            .append("Дальние попытки смешанной | Дальние попытки чистой | ")
            .append("Снятая Тактиком шкала за ход | ")
            .append("Предел Отброса |\n")
            .append("|---|---:|---:|---:|---:|---:|---:|---|\n");
        for (final var result : results) {
            builder.append("| `").append(result.cell().code()).append("` | ")
                .append(percent(result.mixedWinRate())).append(" | ")
                .append(result.referenceWinRate().map(Step12MixedBuildSimulator::percent).orElse("—"))
                .append(" | ").append(percent(result.firstSideWinRate())).append(" | ")
                .append(result.mixedFarAttemptShare().map(Step12MixedBuildSimulator::percent).orElse("—"))
                .append(" | ")
                .append(result.referenceFarAttemptShare().map(Step12MixedBuildSimulator::percent).orElse("—"))
                .append(" | ")
                .append(result.controlReferenceMetric().map(Step12MixedBuildSimulator::number).orElse("—"))
                .append(" | ")
                .append(result.knockbackLimitKept() ? "да" : "нет").append(" |\n");
        }
        if (!failures.isEmpty()) {
            builder.append(finalMode
                ? "\n## Непринятые условия\n\n"
                : "\n## Предварительные отклонения (не решение о балансе)\n\n");
            failures.forEach(failure -> builder.append("- ").append(failure).append('\n'));
        }
        return builder.toString();
    }

    private static void appendGate(
        StringBuilder builder,
        Cell cell,
        String label,
        Gate gate,
        Map<String, AdjustedHypothesis> adjustedByCode
    ) {
        final var adjusted = adjustedByCode.get(gate.code());
        builder.append("| `").append(cell.code()).append("` | ").append(cell.level())
            .append(" | ").append(label).append(" | ")
            .append(number(gate.first())).append(" | ").append(number(gate.second())).append(" | ")
            .append(number(gate.estimate().point())).append(" [")
            .append(number(gate.estimate().lower())).append("; ")
            .append(number(gate.estimate().upper())).append("] | ")
            .append(adjusted == null ? "—" : number(adjusted.adjustedPValue())).append(" | ")
            .append(adjusted != null && adjusted.accepted() ? "принято" : "не принято")
            .append(" |\n");
    }

    private static double mean(double[] values) {
        return java.util.Arrays.stream(values).average().orElseThrow();
    }

    private static String number(double value) {
        return String.format(Locale.ROOT, "%.6f", value);
    }

    private static String exact(double value) {
        return Double.toHexString(value);
    }

    private static String percent(double value) {
        return String.format(Locale.ROOT, "%.2f%%", value * 100);
    }

    private static String property(String name, String defaultValue) {
        return System.getProperty(PREFIX + name, defaultValue);
    }

    private static int intProperty(String name, int defaultValue) {
        return Integer.parseInt(property(name, Integer.toString(defaultValue)));
    }

    private static long longProperty(String name, long defaultValue) {
        return Long.parseLong(property(name, Long.toString(defaultValue)));
    }

    private static boolean booleanProperty(String name, boolean defaultValue) {
        return Boolean.parseBoolean(property(name, Boolean.toString(defaultValue)));
    }

    record Configuration(
        String mode,
        long rootSeed,
        int iterations,
        int maxRounds,
        int workers,
        boolean enforce,
        String scenario,
        String output,
        String statistics
    ) {
    }

    record Gate(String code, double first, double second, Estimate estimate) {
    }

    record CellResult(
        Cell cell,
        Optional<Gate> activity,
        Optional<Gate> price,
        Optional<Gate> control,
        List<Hypothesis> hypotheses,
        double mixedWinRate,
        Optional<Double> referenceWinRate,
        double firstSideWinRate,
        Optional<Double> mixedFarAttemptShare,
        Optional<Double> referenceFarAttemptShare,
        Optional<Double> controlReferenceMetric,
        boolean knockbackLimitKept
    ) {
        CellResult {
            hypotheses = List.copyOf(hypotheses);
            mixedFarAttemptShare = Optional.ofNullable(mixedFarAttemptShare).orElseThrow();
            referenceFarAttemptShare = Optional.ofNullable(referenceFarAttemptShare).orElseThrow();
            controlReferenceMetric = Optional.ofNullable(controlReferenceMetric).orElseThrow();
        }
    }
}
