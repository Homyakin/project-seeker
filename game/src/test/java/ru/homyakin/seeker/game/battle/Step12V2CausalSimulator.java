package ru.homyakin.seeker.game.battle;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import ru.homyakin.seeker.game.battle.Step12CausalObservationRunner.GuardianPairObservation;
import ru.homyakin.seeker.game.battle.Step12CausalObservationRunner.NaturalPairObservation;
import ru.homyakin.seeker.game.battle.Step12CausalObservationRunner.TempoBenchmarkObservation;
import ru.homyakin.seeker.game.battle.Step12CausalStatistics.Estimate;
import ru.homyakin.seeker.game.battle.simulation.PairedCombatSimulator;
import ru.homyakin.seeker.game.battle.simulation.PairedCombatSimulator.PairedCombatReport;
import ru.homyakin.seeker.game.battle.simulation.PairedCombatSimulator.PairedCombatRequest;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.V2Build;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.V2Matchup;

/** Property-gated factorial and causal diagnosis after the rejected step 12 revision-two outcome sample. */
@EnabledIfSystemProperty(named = "step12.causal.enabled", matches = "true")
class Step12V2CausalSimulator {
    private static final String PREFIX = "step12.causal.";
    private static final String REVISION = "V2_CAUSAL_DIAGNOSTIC_1";
    private static final long FINAL_ROOT = 2_026_092_001L;
    private static final int FINAL_ITERATIONS = 10_000;
    private static final int DEFAULT_MAX_ROUNDS = 10_000;
    private static final double PRACTICAL_ZERO = 0.02;
    private static final long FNV_OFFSET_BASIS = 0xcbf29ce484222325L;
    private static final long FNV_PRIME = 0x100000001b3L;
    private static final Pattern MANIFEST_ENTRY = Pattern.compile("([0-9a-f]{64})  (.+)");

    @Test
    void runConfiguredDiagnosis() throws Exception {
        final var configuration = configuration();
        validateConfiguration(configuration);
        final var cells = cells(configuration.rootSeed());
        final var results = runParallel(
            cells,
            configuration.workers(),
            cell -> runCell(cell, configuration.iterations(), configuration.maxRounds())
        );
        final var guardianCells = guardianCells(configuration.rootSeed());
        final var guardianResults = runParallel(
            guardianCells,
            configuration.workers(),
            cell -> runGuardianCell(cell, configuration.iterations(), configuration.maxRounds())
        );
        final var markdown = markdown(configuration, results, guardianResults);
        final var output = Path.of(configuration.output());
        if (output.getParent() != null) {
            Files.createDirectories(output.getParent());
        }
        Files.writeString(output, markdown);
        System.out.println(markdown);
        System.out.println("Report: " + output.toAbsolutePath());

        if (configuration.enforce()) {
            final var failures = new ArrayList<String>();
            results.stream()
                .filter(result -> !result.primary().accepted())
                .map(result -> result.cell().code() + ": " + result.primary().description())
                .forEach(failures::add);
            guardianResults.stream()
                .filter(result -> !result.accepted())
                .map(result -> result.cell().code() + ": " + result.description())
                .forEach(failures::add);
            Assertions.assertTrue(failures.isEmpty(), () -> String.join(System.lineSeparator(), failures));
        }
    }

    private static Configuration configuration() {
        return new Configuration(
            property("mode", "SMOKE").toUpperCase(Locale.ROOT),
            longProperty("seed", 2_026_092_000L),
            intProperty("iterations", 100),
            intProperty("maxRounds", DEFAULT_MAX_ROUNDS),
            intProperty("workers", Math.min(4, Runtime.getRuntime().availableProcessors())),
            booleanProperty("enforce", false),
            property("output", "target/step12-v2-causal-diagnostic.md"),
            property("inputFingerprint", "не зафиксирован"),
            property("inputManifest", "не зафиксирован"),
            property("inputRoot", "не зафиксирован")
        );
    }

    private static void validateConfiguration(Configuration configuration) {
        if (!Set.of("SMOKE", "FINAL").contains(configuration.mode())) {
            throw new IllegalArgumentException("Causal mode must be SMOKE or FINAL");
        }
        if (configuration.iterations() <= 0 || configuration.maxRounds() <= 0) {
            throw new IllegalArgumentException("Iterations and maximum rounds must be positive");
        }
        if (configuration.workers() <= 0) {
            throw new IllegalArgumentException("Workers must be positive");
        }
        if (configuration.mode().equals("SMOKE") && configuration.rootSeed() == FINAL_ROOT) {
            throw new IllegalArgumentException("The final causal root is reserved for FINAL mode");
        }
        if (configuration.mode().equals("FINAL")) {
            final var violations = new ArrayList<String>();
            if (configuration.rootSeed() != FINAL_ROOT) {
                violations.add("seed must equal " + FINAL_ROOT);
            }
            if (configuration.iterations() != FINAL_ITERATIONS) {
                violations.add("iterations must equal " + FINAL_ITERATIONS);
            }
            if (configuration.maxRounds() != DEFAULT_MAX_ROUNDS) {
                violations.add("maxRounds must equal " + DEFAULT_MAX_ROUNDS);
            }
            if (!configuration.enforce()) {
                violations.add("enforce must be true");
            }
            if (!configuration.inputFingerprint().matches("[0-9a-f]{64}")) {
                violations.add("inputFingerprint must be a lowercase SHA-256 value");
            }
            if (configuration.inputManifest().isBlank()) {
                violations.add("inputManifest must not be blank");
            }
            if (configuration.inputRoot().isBlank()) {
                violations.add("inputRoot must not be blank");
            }
            if (!violations.isEmpty()) {
                throw new IllegalArgumentException(
                    "Invalid final causal configuration: " + String.join("; ", violations)
                );
            }
            validateInputManifest(configuration);
        }
    }

    private static void validateInputManifest(Configuration configuration) {
        final var manifest = Path.of(configuration.inputManifest()).toAbsolutePath().normalize();
        final var root = Path.of(configuration.inputRoot()).toAbsolutePath().normalize();
        if (!Files.isRegularFile(manifest)) {
            throw new IllegalArgumentException("Input manifest is not a regular file: " + manifest);
        }
        if (!Files.isDirectory(root)) {
            throw new IllegalArgumentException("Input root is not a directory: " + root);
        }
        final List<ManifestEntry> entries;
        try {
            entries = Files.readAllLines(manifest, StandardCharsets.UTF_8).stream()
                .map(String::strip)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .map(Step12V2CausalSimulator::manifestEntry)
                .toList();
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Cannot read input manifest: " + manifest, exception);
        }
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("Input manifest must contain at least one entry");
        }
        final var sorted = entries.stream()
            .sorted(java.util.Comparator.comparing(ManifestEntry::relativePath))
            .toList();
        if (!entries.equals(sorted)) {
            throw new IllegalArgumentException("Input manifest entries must be sorted by path");
        }
        if (entries.stream().map(ManifestEntry::relativePath).distinct().count() != entries.size()) {
            throw new IllegalArgumentException("Input manifest must not contain duplicate paths");
        }
        final var payload = entries.stream()
            .map(entry -> entry.sha256() + "  " + entry.relativePath())
            .collect(java.util.stream.Collectors.joining("\n", "", "\n"));
        final var manifestFingerprint = sha256(payload.getBytes(StandardCharsets.UTF_8));
        if (!manifestFingerprint.equals(configuration.inputFingerprint())) {
            throw new IllegalArgumentException(
                "Input fingerprint does not match manifest: " + manifestFingerprint
            );
        }
        for (final var entry : entries) {
            final var relative = Path.of(entry.relativePath());
            if (relative.isAbsolute()) {
                throw new IllegalArgumentException("Manifest path must be relative: " + relative);
            }
            final var input = root.resolve(relative).normalize();
            if (!input.startsWith(root) || !Files.isRegularFile(input)) {
                throw new IllegalArgumentException("Manifest input is outside the root or missing: " + input);
            }
            final String actual;
            try {
                actual = sha256(Files.readAllBytes(input));
            } catch (java.io.IOException exception) {
                throw new IllegalStateException("Cannot read manifest input: " + input, exception);
            }
            if (!actual.equals(entry.sha256())) {
                throw new IllegalArgumentException("Manifest checksum mismatch: " + entry.relativePath());
            }
        }
    }

    private static ManifestEntry manifestEntry(String line) {
        final var matcher = MANIFEST_ENTRY.matcher(line);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid input manifest entry: " + line);
        }
        return new ManifestEntry(matcher.group(1), matcher.group(2));
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static List<CausalCell> cells(long rootSeed) {
        final var result = new ArrayList<CausalCell>();
        for (final var matchup : V2Matchup.values()) {
            for (final int level : Step12SimulationFixtures.CONTROL_LEVELS) {
                final var code = matchup.code() + "_ПРИЧИНА_L" + level;
                result.add(new CausalCell(
                    code,
                    matchup,
                    level,
                    cellSeed(rootSeed, code),
                    rolePair(matchup)
                ));
            }
        }
        return List.copyOf(result);
    }

    private static List<GuardianCell> guardianCells(long rootSeed) {
        final var result = new ArrayList<GuardianCell>();
        for (final int partySize : List.of(3, 7)) {
            for (final int level : Step12SimulationFixtures.CONTROL_LEVELS) {
                final var code = "СТРАЖ_ПРИЧИНА_N%d_L%d".formatted(partySize, level);
                result.add(new GuardianCell(code, partySize, level, cellSeed(rootSeed, code)));
            }
        }
        return List.copyOf(result);
    }

    private static CellResult runCell(CausalCell cell, int iterations, int maxRounds) {
        final var reports = new EnumMap<FactorialWorld, PairedCombatReport>(FactorialWorld.class);
        for (final var world : FactorialWorld.values()) {
            reports.put(world, new PairedCombatSimulator().run(request(cell, world, iterations, maxRounds)));
        }
        final var effects = effects(cell, reports);
        final var variants = new EnumMap<FactorialWorld, VariantResult>(FactorialWorld.class);
        reports.forEach((world, report) -> variants.put(
            world,
            new VariantResult(
                report.evaluatedWinRate(),
                report.evaluatedWinRate95().lower(),
                report.evaluatedWinRate95().upper(),
                report.firstSideWinRate()
            )
        ));
        final var primary = primary(cell, iterations, maxRounds);
        return new CellResult(cell, Map.copyOf(variants), effects, primary);
    }

    private static PairedCombatRequest request(
        CausalCell cell,
        FactorialWorld world,
        int iterations,
        int maxRounds
    ) {
        final var disabled = world.disabled(cell.roles());
        return new PairedCombatRequest(
            "CAUSAL_FACTORIAL",
            cell.code() + "_" + world.code(),
            cell.roles().evaluated().displayName(),
            cell.roles().opponent().displayName(),
            3,
            iterations,
            maxRounds,
            cell.seed(),
            () -> Step12SimulationFixtures.v2Teams(cell.matchup(), cell.level(), disabled)
        );
    }

    private static List<EffectResult> effects(
        CausalCell cell,
        Map<FactorialWorld, PairedCombatReport> reports
    ) {
        final var both = scores(reports.get(FactorialWorld.BOTH_ON));
        final var evaluatedOff = scores(reports.get(FactorialWorld.EVALUATED_OFF));
        final var opponentOff = scores(reports.get(FactorialWorld.OPPONENT_OFF));
        final var bothOff = scores(reports.get(FactorialWorld.BOTH_OFF));
        final var contrasts = List.of(
            new EffectContrast("умение A при включённом B", difference(both, evaluatedOff)),
            new EffectContrast("умение A при выключенном B", difference(opponentOff, bothOff)),
            new EffectContrast("умение B при включённом A", difference(both, opponentOff)),
            new EffectContrast("умение B при выключенном A", difference(evaluatedOff, bothOff)),
            new EffectContrast("оба умения вместе", difference(both, bothOff)),
            new EffectContrast(
                "взаимодействие A и B",
                interactionDifference(both, evaluatedOff, opponentOff, bothOff)
            )
        );
        final var pValues = new double[contrasts.size()];
        for (int index = 0; index < contrasts.size(); index++) {
            final var contrast = contrasts.get(index);
            pValues[index] = Step12CausalStatistics.pairedSignFlipPValue(
                contrast.differences(),
                streamSeed(cell.seed(), "effect-p-" + index)
            );
        }
        final var adjusted = Step12CausalStatistics.holmAdjustedPValues(pValues);
        final var result = new ArrayList<EffectResult>(contrasts.size());
        for (int index = 0; index < contrasts.size(); index++) {
            result.add(new EffectResult(
                contrasts.get(index).name(),
                Step12CausalStatistics.mean(
                    contrasts.get(index).differences(),
                    streamSeed(cell.seed(), "effect-ci-" + index)
                ),
                pValues[index],
                adjusted[index]
            ));
        }
        return List.copyOf(result);
    }

    private static PrimaryResult primary(CausalCell cell, int iterations, int maxRounds) {
        return switch (cell.matchup()) {
            case BRUISER_V2 -> bruiserPrimary(cell, iterations, maxRounds);
            case SKIRMISHER_V2 -> binaryPrimary(
                cell,
                iterations,
                index -> Step12CausalObservationRunner.skirmisherPair(
                    cell.level(), cell.seed(), index, maxRounds
                ),
                "первый отход отнимает следующую попытку и заряд Разрушителя"
            );
            case ASSASSIN_V2 -> assassinPrimary(cell, iterations, maxRounds);
            case RANGER_V2 -> naturalMeanPrimary(
                cell,
                iterations,
                maxRounds,
                NaturalPairObservation::rangerFarAttemptShare,
                "доля обычных попыток Дальнобойца с дистанции 3–4"
            );
            case BREAKER_V2 -> naturalMeanPrimary(
                cell,
                iterations,
                maxRounds,
                NaturalPairObservation::breakerDischargeScore,
                "доля начальных значений хотя бы с одним разрядом"
            );
            case TACTICIAN_V2 -> tacticianPrimary(cell, iterations, maxRounds);
        };
    }

    private static PrimaryResult bruiserPrimary(CausalCell cell, int iterations, int maxRounds) {
        final var bruiserDamage = new double[iterations];
        final var bruiserAttempts = new double[iterations];
        final var guardianDamage = new double[iterations];
        final var guardianAttempts = new double[iterations];
        for (int index = 0; index < iterations; index++) {
            final var observation = Step12CausalObservationRunner.naturalPair(
                cell.matchup(), cell.level(), cell.seed(), index, maxRounds
            );
            bruiserDamage[index] = observation.bruiserDamage();
            bruiserAttempts[index] = observation.bruiserAttempts();
            guardianDamage[index] = observation.guardianDamage();
            guardianAttempts[index] = observation.guardianAttempts();
        }
        final var ratio = Step12CausalStatistics.ratioOfRatios(
            bruiserDamage,
            bruiserAttempts,
            guardianDamage,
            guardianAttempts,
            streamSeed(cell.seed(), "bruiser-ratio")
        );
        final var damageDifference = Step12CausalStatistics.pairedDifference(
            bruiserDamage,
            guardianDamage,
            streamSeed(cell.seed(), "bruiser-damage")
        );
        final double damagePValue = Step12CausalStatistics.pairedSignFlipPValue(
            bruiserDamage,
            guardianDamage,
            streamSeed(cell.seed(), "bruiser-damage-p")
        );
        final boolean accepted = ratio.confidenceInterval().lower() >= 1.10
            && damageDifference.confidenceInterval().lower() > 0
            && damagePValue <= 0.05;
        return new PrimaryResult(
            ratio,
            accepted,
            String.format(
                Locale.ROOT,
                "отношение урона на попытку; нижняя граница ≥1,10; разность общего урона %.2f ",
                damageDifference.estimate()
            )
                + interval(damageDifference)
                + String.format(Locale.ROOT, "; p=%.5f", damagePValue),
            Optional.empty()
        );
    }

    private static PrimaryResult tacticianPrimary(CausalCell cell, int iterations, int maxRounds) {
        final var applications = new double[iterations];
        for (int index = 0; index < iterations; index++) {
            applications[index] = Step12CausalObservationRunner.naturalPair(
                cell.matchup(),
                cell.level(),
                cell.seed(),
                index,
                maxRounds
            ).tacticianApplicationScore();
        }
        final var estimate = Step12CausalStatistics.mean(
            applications,
            streamSeed(cell.seed(), "primary")
        );
        final var benchmark = Step12CausalObservationRunner.tempoBenchmark(
            cell.level(),
            streamSeed(cell.seed(), "tempo-benchmark"),
            10_000,
            100_000
        );
        final boolean applicationAccepted = thresholdAccepted(estimate);
        final boolean benchmarkAccepted = tempoBenchmarkAccepted(benchmark);
        return new PrimaryResult(
            estimate,
            applicationAccepted && benchmarkAccepted,
            "доля начальных значений с применением к названному Громиле; нижняя граница ≥75%, "
                + "ширина ≤2 п.п.; точное снятие 83; потеря действий в длинном опыте (0; 15%]",
            Optional.of(benchmark)
        );
    }

    private static PrimaryResult assassinPrimary(CausalCell cell, int iterations, int maxRounds) {
        final var exact = exactBinaryPrimary(
            cell,
            iterations,
            index -> Step12CausalObservationRunner.assassinPair(cell.level(), cell.seed(), index),
            "управляемое дальнее окно даёт попытку только с «Проникновением»"
        );
        final var naturalAccess = new double[iterations];
        for (int index = 0; index < iterations; index++) {
            naturalAccess[index] = Step12CausalObservationRunner.naturalPair(
                cell.matchup(),
                cell.level(),
                cell.seed(),
                index,
                maxRounds
            ).assassinNaturalAccessScore();
        }
        final var naturalEstimate = Step12CausalStatistics.mean(
            naturalAccess,
            streamSeed(cell.seed(), "assassin-natural-access")
        );
        return new PrimaryResult(
            exact.estimate(),
            exact.accepted(),
            exact.description() + "; естественный доступ " + percentEstimate(naturalEstimate),
            Optional.empty()
        );
    }

    private static PrimaryResult naturalMeanPrimary(
        CausalCell cell,
        int iterations,
        int maxRounds,
        java.util.function.ToDoubleFunction<NaturalPairObservation> extractor,
        String description
    ) {
        final var observations = new double[iterations];
        for (int index = 0; index < iterations; index++) {
            observations[index] = extractor.applyAsDouble(Step12CausalObservationRunner.naturalPair(
                cell.matchup(), cell.level(), cell.seed(), index, maxRounds
            ));
        }
        return thresholdPrimary(
            Step12CausalStatistics.mean(observations, streamSeed(cell.seed(), "primary")),
            description
        );
    }

    private static PrimaryResult binaryPrimary(
        CausalCell cell,
        int iterations,
        java.util.function.IntToDoubleFunction observation,
        String description
    ) {
        final var observations = new double[iterations];
        for (int index = 0; index < iterations; index++) {
            observations[index] = observation.applyAsDouble(index);
        }
        return thresholdPrimary(
            Step12CausalStatistics.mean(observations, streamSeed(cell.seed(), "primary")),
            description
        );
    }

    private static PrimaryResult exactBinaryPrimary(
        CausalCell cell,
        int iterations,
        java.util.function.IntToDoubleFunction observation,
        String description
    ) {
        final var observations = new double[iterations];
        var accepted = true;
        for (int index = 0; index < iterations; index++) {
            observations[index] = observation.applyAsDouble(index);
            accepted &= observations[index] == 1.0;
        }
        final var estimate = Step12CausalStatistics.mean(
            observations,
            streamSeed(cell.seed(), "primary")
        );
        return new PrimaryResult(
            estimate,
            accepted,
            description + "; требуется каждое управляемое состояние",
            Optional.empty()
        );
    }

    private static PrimaryResult thresholdPrimary(Estimate estimate, String description) {
        final boolean accepted = thresholdAccepted(estimate);
        return new PrimaryResult(
            estimate,
            accepted,
            description + "; нижняя граница ≥75%, ширина ≤2 п.п.",
            Optional.empty()
        );
    }

    private static boolean thresholdAccepted(Estimate estimate) {
        return estimate.confidenceInterval().lower() >= 0.75
            && estimate.confidenceInterval().width() <= 0.02 + 1e-12;
    }

    private static GuardianResult runGuardianCell(GuardianCell cell, int iterations, int maxRounds) {
        final var useful = new double[iterations];
        final var damageReduction = new double[iterations];
        final var turnIncrease = new double[iterations];
        var capKept = true;
        for (int index = 0; index < iterations; index++) {
            final GuardianPairObservation observation = Step12CausalObservationRunner.guardianPair(
                cell.partySize(), cell.level(), cell.seed(), index, maxRounds
            );
            useful[index] = observation.usefulInterceptionScore();
            damageReduction[index] = observation.wardDamageReduction();
            turnIncrease[index] = observation.wardTurnIncrease();
            capKept &= observation.interceptionLimitKept();
        }
        final var usefulEstimate = Step12CausalStatistics.mean(
            useful,
            streamSeed(cell.seed(), "guardian-useful")
        );
        final var damageEstimate = Step12CausalStatistics.mean(
            damageReduction,
            streamSeed(cell.seed(), "guardian-damage")
        );
        final var turnEstimate = Step12CausalStatistics.mean(
            turnIncrease,
            streamSeed(cell.seed(), "guardian-turns")
        );
        final var pValues = new double[] {
            Step12CausalStatistics.pairedSignFlipPValue(
                damageReduction,
                streamSeed(cell.seed(), "guardian-damage-p")
            ),
            Step12CausalStatistics.pairedSignFlipPValue(
                turnIncrease,
                streamSeed(cell.seed(), "guardian-turns-p")
            ),
        };
        final var adjustedPValues = Step12CausalStatistics.holmAdjustedPValues(pValues);
        final boolean accepted = usefulEstimate.confidenceInterval().lower() >= 0.75
            && usefulEstimate.confidenceInterval().width() <= 0.02 + 1e-12
            && damageEstimate.confidenceInterval().lower() > 0
            && turnEstimate.confidenceInterval().lower() > 0
            && adjustedPValues[0] <= 0.05
            && adjustedPValues[1] <= 0.05
            && capKept;
        return new GuardianResult(
            cell,
            usefulEstimate,
            damageEstimate,
            turnEstimate,
            pValues[0],
            adjustedPValues[0],
            pValues[1],
            adjustedPValues[1],
            capKept,
            accepted,
            "полезный перехват ≥75%, урон союзнику ниже, действий союзника больше, предел соблюдён"
        );
    }

    private static <T, R> List<R> runParallel(
        List<T> values,
        int workers,
        java.util.function.Function<T, R> operation
    ) {
        if (workers == 1 || values.size() == 1) {
            return values.stream().map(operation).toList();
        }
        final var pool = new ForkJoinPool(Math.min(workers, values.size()));
        try {
            return pool.submit(() -> values.parallelStream().map(operation).toList()).get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Causal diagnosis was interrupted", exception);
        } catch (ExecutionException exception) {
            final var cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException("Causal diagnosis failed", cause);
        } finally {
            pool.shutdown();
        }
    }

    private static String markdown(
        Configuration configuration,
        List<CellResult> results,
        List<GuardianResult> guardianResults
    ) {
        final var output = new StringBuilder()
            .append("# Причинная диагностика второй редакции шага 12\n\n")
            .append("Код: `").append(REVISION).append("`; корень: `")
            .append(configuration.rootSeed()).append("`; начальных значений на строку: `")
            .append(configuration.iterations()).append("`; два порядка сторон. ")
            .append("Отпечаток входов: `").append(configuration.inputFingerprint()).append("`. ")
            .append("Диагностика не является повторной приёмкой итоговой выборки V2.\n\n")
            .append("## Разложение доли побед\n\n")
            .append("| Ячейка | `++` | `-+` | `+-` | `--` | Первичный показатель | Маршрут |\n")
            .append("|---|---:|---:|---:|---:|---|---|\n");
        for (final var result : results) {
            output.append("| ").append(result.cell().code()).append(" | ")
                .append(variant(result.variants().get(FactorialWorld.BOTH_ON))).append(" | ")
                .append(variant(result.variants().get(FactorialWorld.EVALUATED_OFF))).append(" | ")
                .append(variant(result.variants().get(FactorialWorld.OPPONENT_OFF))).append(" | ")
                .append(variant(result.variants().get(FactorialWorld.BOTH_OFF))).append(" | ")
                .append(primaryEstimate(result)).append(" — ")
                .append(mark(result.primary().accepted())).append(" | ")
                .append(route(result)).append(" |\n");
        }
        output.append("\n`++`: оба умения включены; `-+`: выключено умение A; `+-`: выключено умение B; ")
            .append("`--`: выключены оба.\n\n")
            .append("## Связанные вклады умений в долю побед\n\n")
            .append("| Ячейка | Сравнение | Разность и 95-процентный интервал | p | p Холма |\n")
            .append("|---|---|---:|---:|---:|\n");
        for (final var result : results) {
            for (final var effect : result.effects()) {
                output.append(String.format(
                    Locale.ROOT,
                    "| %s | %s | %s | %.5f | %.5f |%n",
                    result.cell().code(),
                    effect.name(),
                    percentEstimate(effect.estimate()),
                    effect.pValue(),
                    effect.holmPValue()
                ));
            }
        }
        output.append("\n## Длинный опыт Тактика\n\n")
            .append("| Ячейка | Действия Громилы с умением | Контроль | Потеря | Итог |\n")
            .append("|---|---:|---:|---:|---|\n");
        for (final var result : results) {
            result.primary().tempoBenchmark().ifPresent(benchmark -> output.append(String.format(
                Locale.ROOT,
                "| %s | %d | %d | %.3f%% | %s |%n",
                result.cell().code(),
                benchmark.activeBruiserTurns(),
                benchmark.controlBruiserTurns(),
                benchmark.relativeLoss() * 100,
                mark(tempoBenchmarkAccepted(benchmark))
            )));
        }
        output.append("\n## Страж\n\n")
            .append("| Ячейка | Полезный перехват | Снижение урона | p | p Холма | ")
            .append("Прибавка действий | p | p Холма | Предел | Итог | Маршрут |\n")
            .append("|---|---:|---:|---:|---:|---:|---:|---:|---|---|---|\n");
        for (final var result : guardianResults) {
            output.append("| ").append(result.cell().code()).append(" | ")
                .append(percentEstimate(result.useful())).append(" | ")
                .append(numberEstimate(result.damageReduction())).append(" | ")
                .append(String.format(Locale.ROOT, "%.5f", result.damagePValue())).append(" | ")
                .append(String.format(Locale.ROOT, "%.5f", result.damageHolmPValue())).append(" | ")
                .append(numberEstimate(result.turnIncrease())).append(" | ")
                .append(String.format(Locale.ROOT, "%.5f", result.turnPValue())).append(" | ")
                .append(String.format(Locale.ROOT, "%.5f", result.turnHolmPValue())).append(" | ")
                .append(mark(result.capKept())).append(" | ")
                .append(mark(result.accepted())).append(" | ")
                .append(result.accepted() ? "не требуется" : "шаг 6").append(" |\n");
        }
        output.append("\n## Итоговый маршрут ролей\n\n")
            .append("| Роль | Маршруты уровней | Итог |\n")
            .append("|---|---|---|\n");
        for (final var matchup : V2Matchup.values()) {
            final var roleResults = results.stream()
                .filter(result -> result.cell().matchup() == matchup)
                .toList();
            output.append("| ").append(rolePair(matchup).evaluated().displayName()).append(" | ")
                .append(roleResults.stream()
                    .map(result -> "+" + result.cell().level() + ": " + route(result))
                    .collect(java.util.stream.Collectors.joining("; ")))
                .append(" | ").append(roleRoute(roleResults)).append(" |\n");
        }
        output.append("| Страж | ")
            .append(guardianResults.stream()
                .map(result -> result.cell().partySize() + "×" + result.cell().partySize()
                    + " +" + result.cell().level() + ": "
                    + (result.accepted() ? "не требуется" : "шаг 6"))
                .collect(java.util.stream.Collectors.joining("; ")))
            .append(" | ")
            .append(guardianResults.stream().allMatch(GuardianResult::accepted) ? "не требуется" : "шаг 6")
            .append(" |\n")
            .append("\n## Определения первичных показателей\n\n");
        for (final var result : results) {
            output.append("- `").append(result.cell().code()).append("`: ")
                .append(result.primary().description()).append("\n");
        }
        return output.toString();
    }

    private static String variant(VariantResult result) {
        return String.format(
            Locale.ROOT,
            "%.2f%% [%.2f%%; %.2f%%]",
            result.winRate() * 100,
            result.lower() * 100,
            result.upper() * 100
        );
    }

    private static String percentEstimate(Estimate estimate) {
        return String.format(
            Locale.ROOT,
            "%.2f%% [%.2f%%; %.2f%%]",
            estimate.estimate() * 100,
            estimate.confidenceInterval().lower() * 100,
            estimate.confidenceInterval().upper() * 100
        );
    }

    private static String primaryEstimate(CellResult result) {
        return result.cell().matchup() == V2Matchup.BRUISER_V2
            ? numberEstimate(result.primary().estimate())
            : percentEstimate(result.primary().estimate());
    }

    private static String numberEstimate(Estimate estimate) {
        return String.format(
            Locale.ROOT,
            "%.2f [%.2f; %.2f]",
            estimate.estimate(),
            estimate.confidenceInterval().lower(),
            estimate.confidenceInterval().upper()
        );
    }

    private static String interval(Estimate estimate) {
        return String.format(
            Locale.ROOT,
            " [%.2f; %.2f]",
            estimate.confidenceInterval().lower(),
            estimate.confidenceInterval().upper()
        );
    }

    private static String mark(boolean value) {
        return value ? "принято" : "не принято";
    }

    private static boolean tempoBenchmarkAccepted(TempoBenchmarkObservation benchmark) {
        final long lostTurns = benchmark.controlBruiserTurns() - benchmark.activeBruiserTurns();
        return benchmark.controlBruiserTurns() == 32_000
            && lostTurns > 0
            && Math.multiplyExact(20L, lostTurns)
                <= Math.multiplyExact(3L, benchmark.controlBruiserTurns());
    }

    private static String route(CellResult result) {
        if (!result.primary().accepted()) {
            return "шаг 6";
        }
        final var full = result.variants().get(FactorialWorld.BOTH_ON);
        final var baseline = result.variants().get(FactorialWorld.BOTH_OFF);
        final var fullPosition = outcomePosition(full, result.cell().level());
        final var baselinePosition = outcomePosition(baseline, result.cell().level());
        if (fullPosition == OutcomePosition.ACCEPTED) {
            return "не требуется";
        }
        final var total = result.effects().stream()
            .filter(effect -> effect.name().equals("оба умения вместе"))
            .findFirst()
            .orElseThrow();
        final var interaction = result.effects().stream()
            .filter(effect -> effect.name().equals("взаимодействие A и B"))
            .findFirst()
            .orElseThrow();
        final boolean interactionEquivalent = practicallyZero(interaction);
        if (baselinePosition == OutcomePosition.ACCEPTED) {
            return interactionEquivalent && movesAway(total, fullPosition) ? "шаг 6" : "смешанный";
        }
        final boolean sameClearSide = fullPosition == baselinePosition
            && Set.of(OutcomePosition.BELOW, OutcomePosition.ABOVE).contains(fullPosition);
        if (sameClearSide
            && interactionEquivalent
            && (practicallyZero(total) || movesToward(total, fullPosition))
            && conditionalEffectsDoNotHarm(result.effects(), fullPosition)) {
            return "шаг 7";
        }
        return "смешанный";
    }

    private static boolean conditionalEffectsDoNotHarm(
        List<EffectResult> effects,
        OutcomePosition position
    ) {
        return effects.subList(0, 4).stream()
            .allMatch(effect -> practicallyZero(effect) || movesToward(effect, position));
    }

    private static String roleRoute(List<CellResult> results) {
        if (results.stream().anyMatch(result -> !result.primary().accepted())) {
            return "шаг 6";
        }
        final var requiredRoutes = results.stream()
            .map(Step12V2CausalSimulator::route)
            .distinct()
            .toList();
        if (requiredRoutes.size() == 1) {
            return requiredRoutes.getFirst();
        }
        return "смешанный";
    }

    private static boolean practicallyZero(EffectResult effect) {
        return effect.estimate().confidenceInterval().lower() >= -PRACTICAL_ZERO
            && effect.estimate().confidenceInterval().upper() <= PRACTICAL_ZERO;
    }

    private static boolean movesToward(EffectResult effect, OutcomePosition position) {
        return switch (position) {
            case BELOW -> effect.estimate().confidenceInterval().lower() > 0;
            case ABOVE -> effect.estimate().confidenceInterval().upper() < 0;
            case ACCEPTED, UNCERTAIN -> false;
        };
    }

    private static boolean movesAway(EffectResult effect, OutcomePosition position) {
        return switch (position) {
            case BELOW -> effect.estimate().confidenceInterval().upper() < 0;
            case ABOVE -> effect.estimate().confidenceInterval().lower() > 0;
            case ACCEPTED, UNCERTAIN -> false;
        };
    }

    private static OutcomePosition outcomePosition(VariantResult result, int level) {
        if (level == 3 || level == 6) {
            if (result.lower() > 0.50) {
                return OutcomePosition.ACCEPTED;
            }
            return result.upper() <= 0.50 ? OutcomePosition.BELOW : OutcomePosition.UNCERTAIN;
        }
        if (result.lower() >= 0.55 && result.upper() <= 0.65) {
            return OutcomePosition.ACCEPTED;
        }
        if (result.upper() < 0.55) {
            return OutcomePosition.BELOW;
        }
        return result.lower() > 0.65 ? OutcomePosition.ABOVE : OutcomePosition.UNCERTAIN;
    }

    private static double[] scores(PairedCombatReport report) {
        return report.pairOutcomes().stream().mapToDouble(PairedCombatSimulator.PairOutcome::score).toArray();
    }

    private static double[] difference(double[] first, double[] second) {
        if (first.length != second.length) {
            throw new IllegalArgumentException("Paired causal worlds must have the same size");
        }
        final var result = new double[first.length];
        for (int index = 0; index < result.length; index++) {
            result[index] = first[index] - second[index];
        }
        return result;
    }

    private static double[] interactionDifference(
        double[] both,
        double[] evaluatedOff,
        double[] opponentOff,
        double[] bothOff
    ) {
        if (both.length != evaluatedOff.length
            || both.length != opponentOff.length
            || both.length != bothOff.length) {
            throw new IllegalArgumentException("Factorial causal worlds must have the same size");
        }
        final var result = new double[both.length];
        for (int index = 0; index < result.length; index++) {
            result[index] = both[index] - evaluatedOff[index] - opponentOff[index] + bothOff[index];
        }
        return result;
    }

    private static V2RolePair rolePair(V2Matchup matchup) {
        return switch (matchup) {
            case BRUISER_V2 -> new V2RolePair(V2Build.BRUISER_BLUNT, V2Build.GUARDIAN_BLUNT);
            case SKIRMISHER_V2 -> new V2RolePair(V2Build.SKIRMISHER_SLASH, V2Build.BREAKER_CLOSE_SLASH);
            case ASSASSIN_V2 -> new V2RolePair(V2Build.ASSASSIN_PIERCE, V2Build.RANGER_PIERCE);
            case RANGER_V2 -> new V2RolePair(V2Build.RANGER_MAGICAL, V2Build.BRUISER_MAGICAL_CLOTH);
            case BREAKER_V2 -> new V2RolePair(V2Build.BREAKER_MAGICAL_LEATHER, V2Build.BRUISER_MAGICAL_LEATHER);
            case TACTICIAN_V2 -> new V2RolePair(V2Build.TACTICIAN_MAGICAL, V2Build.BRUISER_MAGICAL_CLOTH);
        };
    }

    static long cellSeed(long rootSeed, String cellCode) {
        return streamSeed(rootSeed, cellCode);
    }

    private static long streamSeed(long rootSeed, String stream) {
        var hash = FNV_OFFSET_BASIS;
        for (int index = 0; index < stream.length(); index++) {
            hash ^= stream.charAt(index);
            hash *= FNV_PRIME;
        }
        var mixed = rootSeed ^ hash;
        mixed = (mixed ^ mixed >>> 30) * 0xbf58476d1ce4e5b9L;
        mixed = (mixed ^ mixed >>> 27) * 0x94d049bb133111ebL;
        return mixed ^ mixed >>> 31;
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

    private enum FactorialWorld {
        BOTH_ON("++"),
        EVALUATED_OFF("-+"),
        OPPONENT_OFF("+-"),
        BOTH_OFF("--");

        private final String code;

        FactorialWorld(String code) {
            this.code = code;
        }

        private String code() {
            return code;
        }

        private Set<V2Build> disabled(V2RolePair roles) {
            return switch (this) {
                case BOTH_ON -> Set.of();
                case EVALUATED_OFF -> Set.of(roles.evaluated());
                case OPPONENT_OFF -> Set.of(roles.opponent());
                case BOTH_OFF -> Set.of(roles.evaluated(), roles.opponent());
            };
        }
    }

    private enum OutcomePosition {
        ACCEPTED,
        BELOW,
        ABOVE,
        UNCERTAIN,
    }

    private record Configuration(
        String mode,
        long rootSeed,
        int iterations,
        int maxRounds,
        int workers,
        boolean enforce,
        String output,
        String inputFingerprint,
        String inputManifest,
        String inputRoot
    ) {
        private Configuration {
            Objects.requireNonNull(mode);
            Objects.requireNonNull(output);
            Objects.requireNonNull(inputFingerprint);
            Objects.requireNonNull(inputManifest);
            Objects.requireNonNull(inputRoot);
        }
    }

    private record ManifestEntry(String sha256, String relativePath) {
    }

    private record CausalCell(
        String code,
        V2Matchup matchup,
        int level,
        long seed,
        V2RolePair roles
    ) {
    }

    private record GuardianCell(String code, int partySize, int level, long seed) {
    }

    private record V2RolePair(V2Build evaluated, V2Build opponent) {
    }

    private record VariantResult(double winRate, double lower, double upper, double firstSideWinRate) {
    }

    private record EffectContrast(String name, double[] differences) {
    }

    private record EffectResult(String name, Estimate estimate, double pValue, double holmPValue) {
    }

    private record PrimaryResult(
        Estimate estimate,
        boolean accepted,
        String description,
        Optional<TempoBenchmarkObservation> tempoBenchmark
    ) {
        private PrimaryResult {
            Objects.requireNonNull(tempoBenchmark);
        }
    }

    private record CellResult(
        CausalCell cell,
        Map<FactorialWorld, VariantResult> variants,
        List<EffectResult> effects,
        PrimaryResult primary
    ) {
    }

    private record GuardianResult(
        GuardianCell cell,
        Estimate useful,
        Estimate damageReduction,
        Estimate turnIncrease,
        double damagePValue,
        double damageHolmPValue,
        double turnPValue,
        double turnHolmPValue,
        boolean capKept,
        boolean accepted,
        String description
    ) {
    }

    @FunctionalInterface
    private interface ThrowingCellOperation<T, R> extends java.util.function.Function<T, R> {
    }
}
