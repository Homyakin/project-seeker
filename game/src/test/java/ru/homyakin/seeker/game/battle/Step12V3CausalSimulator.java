package ru.homyakin.seeker.game.battle;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import ru.homyakin.seeker.game.battle.Step12CausalStatistics.Estimate;
import ru.homyakin.seeker.game.battle.Step12V3CausalObservationRunner.GuardianPairObservation;
import ru.homyakin.seeker.game.battle.Step12V3CausalObservationRunner.NaturalPairObservation;
import ru.homyakin.seeker.game.battle.Step12V3CausalObservationRunner.TempoBenchmarkObservation;
import ru.homyakin.seeker.game.battle.Step12V3CausalObservationRunner.TempoExactObservation;
import ru.homyakin.seeker.game.battle.simulation.PairedCombatSimulator;
import ru.homyakin.seeker.game.battle.simulation.PairedCombatSimulator.PairedCombatReport;
import ru.homyakin.seeker.game.battle.simulation.PairedCombatSimulator.PairedCombatRequest;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.V3Build;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.V3Matchup;
import ru.homyakin.seeker.game.battle.simulation.Step12V3AcceptanceLedger;
import ru.homyakin.seeker.game.battle.simulation.Step12V4AcceptanceLedger;

/** V3-candidate causal diagnosis with separately versioned acceptance ledgers. */
@EnabledIfSystemProperty(named = "step12.v3.causal.enabled", matches = "true")
class Step12V3CausalSimulator {
    static final long DIAGNOSTIC_ROOT = 2_026_092_101L;
    static final long OUTCOME_PREFLIGHT_ROOT = 2_026_092_201L;
    static final long CAUSAL_PREFLIGHT_ROOT = 2_026_092_202L;
    static final long FINAL_ROOT = 2_026_092_301L;
    static final long V4_OUTCOME_PREFLIGHT_ROOT = 2_026_092_203L;
    static final long V4_CAUSAL_PREFLIGHT_ROOT = 2_026_092_204L;
    static final long V4_FINAL_ROOT = 2_026_092_302L;

    private static final String PREFIX = "step12.v3.causal.";
    private static final String REVISION = "V3_CAUSAL_1";
    private static final int DEFAULT_ITERATIONS = 100;
    private static final int PREFLIGHT_ITERATIONS = 2_000;
    private static final int FINAL_ITERATIONS = 10_000;
    private static final int DEFAULT_MAX_ROUNDS = 10_000;
    private static final double PRACTICAL_ZERO = 0.02;
    private static final long FNV_OFFSET_BASIS = 0xcbf29ce484222325L;
    private static final long FNV_PRIME = 0x100000001b3L;

    @Test
    void runConfiguredDiagnosis() throws Exception {
        final var configuration = configuration();
        final var matchups = List.of(V3Matchup.values());
        final var levels = Step12SimulationFixtures.CONTROL_LEVELS;
        final var guardianPartySizes = List.of(3, 7);
        validateConfiguration(configuration);
        validateSelection(configuration.mode(), matchups, levels, guardianPartySizes);
        final Step12V3AcceptanceLedger v3Ledger;
        final Step12V3AcceptanceLedger.Attempt v3Attempt;
        final Step12V4AcceptanceLedger v4Ledger;
        final Step12V4AcceptanceLedger.Attempt v4Attempt;
        if (!configuration.mode().equals("DIAGNOSTIC") && configuration.acceptanceProtocol().equals("V3")) {
            rejectLegacyInputProperties();
            v3Ledger = Step12V3AcceptanceLedger.fromTestClasses(Step12V3CausalSimulator.class);
            final var kind = configuration.mode().equals("PREFLIGHT")
                ? Step12V3AcceptanceLedger.RunKind.CAUSAL_PREFLIGHT
                : Step12V3AcceptanceLedger.RunKind.CAUSAL_FINAL;
            v3Attempt = v3Ledger.begin(
                kind,
                configuration.rootSeed(),
                configuration.iterations(),
                configuration.maxRounds(),
                acceptanceCellDescriptors(
                    configuration.rootSeed(),
                    matchups,
                    levels,
                    guardianPartySizes
                ),
                System.getProperty(PREFIX + "output")
            );
            v4Ledger = null;
            v4Attempt = null;
        } else if (!configuration.mode().equals("DIAGNOSTIC")) {
            rejectLegacyInputProperties();
            v4Ledger = Step12V4AcceptanceLedger.fromTestClasses(Step12V3CausalSimulator.class);
            final var kind = configuration.mode().equals("PREFLIGHT")
                ? Step12V4AcceptanceLedger.RunKind.CAUSAL_PREFLIGHT
                : Step12V4AcceptanceLedger.RunKind.CAUSAL_FINAL;
            v4Attempt = v4Ledger.begin(
                kind,
                configuration.rootSeed(),
                configuration.iterations(),
                configuration.maxRounds(),
                acceptanceCellDescriptors(
                    configuration.rootSeed(),
                    matchups,
                    levels,
                    guardianPartySizes
                ),
                System.getProperty(PREFIX + "output")
            );
            v3Ledger = null;
            v3Attempt = null;
        } else {
            v3Ledger = null;
            v3Attempt = null;
            v4Ledger = null;
            v4Attempt = null;
        }
        final var run = runValidated(
            configuration,
            matchups,
            levels,
            guardianPartySizes
        );
        final Path output;
        if (v3Attempt != null) {
            output = v3Attempt.report();
            v3Ledger.writeReport(v3Attempt, run.markdown());
        } else if (v4Attempt != null) {
            output = v4Attempt.report();
            v4Ledger.writeReport(v4Attempt, run.markdown());
        } else {
            output = Path.of(configuration.output());
            if (output.getParent() != null) {
                Files.createDirectories(output.getParent());
            }
            Files.writeString(output, run.markdown());
        }
        System.out.println(run.markdown());
        System.out.println("Report: " + output.toAbsolutePath());

        if (configuration.enforce()) {
            final var failures = new ArrayList<String>();
            run.results().stream()
                .filter(result -> !result.primary().accepted())
                .map(result -> result.cell().code() + ": " + result.primary().description())
                .forEach(failures::add);
            run.guardianResults().stream()
                .filter(result -> !result.accepted())
                .map(result -> result.cell().code() + ": " + result.description())
                .forEach(failures::add);
            Assertions.assertTrue(failures.isEmpty(), () -> String.join(System.lineSeparator(), failures));
        }
        if (v3Attempt != null) {
            v3Ledger.complete(v3Attempt);
        } else if (v4Attempt != null) {
            v4Ledger.complete(v4Attempt);
        }
    }

    static Configuration configuration() {
        return new Configuration(
            property("mode", "DIAGNOSTIC").toUpperCase(Locale.ROOT),
            longProperty("seed", DIAGNOSTIC_ROOT),
            intProperty("iterations", DEFAULT_ITERATIONS),
            intProperty("maxRounds", DEFAULT_MAX_ROUNDS),
            intProperty("workers", Math.min(4, Runtime.getRuntime().availableProcessors())),
            booleanProperty("enforce", false),
            property("output", "target/step12-v3-causal-diagnostic.md"),
            property("acceptance", "V3").toUpperCase(Locale.ROOT)
        );
    }

    static void validateConfiguration(Configuration configuration) {
        if (!Set.of("DIAGNOSTIC", "PREFLIGHT", "FINAL").contains(configuration.mode())) {
            throw new IllegalArgumentException("V3 causal mode must be DIAGNOSTIC, PREFLIGHT or FINAL");
        }
        if (!Set.of("V3", "V4").contains(configuration.acceptanceProtocol())) {
            throw new IllegalArgumentException(
                "Unknown step 12 acceptance protocol: " + configuration.acceptanceProtocol()
            );
        }
        if (Set.of(OUTCOME_PREFLIGHT_ROOT, V4_OUTCOME_PREFLIGHT_ROOT).contains(configuration.rootSeed())) {
            throw new IllegalArgumentException("Outcome preflight roots are forbidden in the causal executor");
        }
        if (configuration.mode().equals("DIAGNOSTIC")
            && Set.of(
                2_026_091_802L,
                OUTCOME_PREFLIGHT_ROOT,
                CAUSAL_PREFLIGHT_ROOT,
                FINAL_ROOT,
                V4_OUTCOME_PREFLIGHT_ROOT,
                V4_CAUSAL_PREFLIGHT_ROOT,
                V4_FINAL_ROOT
            ).contains(configuration.rootSeed())) {
            throw new IllegalArgumentException("Acceptance root seeds are reserved");
        }
        if (configuration.mode().equals("DIAGNOSTIC") && configuration.rootSeed() != DIAGNOSTIC_ROOT) {
            throw new IllegalArgumentException("Diagnostic seed must equal " + DIAGNOSTIC_ROOT);
        }
        final long expectedPreflightRoot = configuration.acceptanceProtocol().equals("V4")
            ? V4_CAUSAL_PREFLIGHT_ROOT
            : CAUSAL_PREFLIGHT_ROOT;
        final long expectedFinalRoot = configuration.acceptanceProtocol().equals("V4")
            ? V4_FINAL_ROOT
            : FINAL_ROOT;
        if (configuration.mode().equals("PREFLIGHT") && configuration.rootSeed() != expectedPreflightRoot) {
            throw new IllegalArgumentException("Causal preflight seed must equal " + expectedPreflightRoot);
        }
        if (configuration.mode().equals("FINAL") && configuration.rootSeed() != expectedFinalRoot) {
            throw new IllegalArgumentException("Causal final seed must equal " + expectedFinalRoot);
        }
        if (configuration.iterations() <= 0 || configuration.maxRounds() <= 0) {
            throw new IllegalArgumentException("Iterations and maximum rounds must be positive");
        }
        if (configuration.workers() <= 0) {
            throw new IllegalArgumentException("Workers must be positive");
        }
        if (configuration.output().isBlank()) {
            throw new IllegalArgumentException("Output must not be blank");
        }
        if (!configuration.mode().equals("DIAGNOSTIC")) {
            final var violations = new ArrayList<String>();
            final int expectedIterations = configuration.mode().equals("PREFLIGHT")
                ? PREFLIGHT_ITERATIONS
                : FINAL_ITERATIONS;
            if (configuration.iterations() != expectedIterations) {
                violations.add("iterations must equal " + expectedIterations);
            }
            if (configuration.maxRounds() != DEFAULT_MAX_ROUNDS) {
                violations.add("maxRounds must equal " + DEFAULT_MAX_ROUNDS);
            }
            if (!configuration.enforce()) {
                violations.add("enforce must be true");
            }
            if (!violations.isEmpty()) {
                throw new IllegalArgumentException(
                    "Invalid " + configuration.acceptanceProtocol() + " acceptance for V3 causal "
                        + configuration.mode() + " configuration: "
                        + String.join("; ", violations)
                );
            }
        }
    }

    static DiagnosticRun run(
        Configuration configuration,
        List<V3Matchup> matchups,
        List<Integer> levels,
        List<Integer> guardianPartySizes
    ) {
        validateConfiguration(configuration);
        validateSelection(configuration.mode(), matchups, levels, guardianPartySizes);
        if (!configuration.mode().equals("DIAGNOSTIC")) {
            throw new IllegalStateException(
                "V3 causal acceptance can run only through the marker-backed configured executor"
            );
        }
        return runValidated(configuration, matchups, levels, guardianPartySizes);
    }

    private static DiagnosticRun runValidated(
        Configuration configuration,
        List<V3Matchup> matchups,
        List<Integer> levels,
        List<Integer> guardianPartySizes
    ) {
        final var cells = cells(configuration.rootSeed(), matchups, levels);
        final var results = runParallel(
            cells,
            configuration.workers(),
            cell -> runCell(
                cell,
                configuration.iterations(),
                configuration.maxRounds(),
                configuration.mode()
            )
        );
        final var guardianCells = guardianCells(
            configuration.rootSeed(),
            levels,
            guardianPartySizes
        );
        final var guardianResults = runParallel(
            guardianCells,
            configuration.workers(),
            cell -> runGuardianCell(
                cell,
                configuration.iterations(),
                configuration.maxRounds(),
                configuration.mode()
            )
        );
        return new DiagnosticRun(
            results,
            guardianResults,
            markdown(configuration, results, guardianResults)
        );
    }

    private static void validateSelection(
        String mode,
        List<V3Matchup> matchups,
        List<Integer> levels,
        List<Integer> guardianPartySizes
    ) {
        Objects.requireNonNull(matchups, "matchups");
        Objects.requireNonNull(levels, "levels");
        Objects.requireNonNull(guardianPartySizes, "guardianPartySizes");
        if (matchups.isEmpty() || levels.isEmpty()) {
            throw new IllegalArgumentException("At least one V3 matchup and level are required");
        }
        if (!Step12SimulationFixtures.CONTROL_LEVELS.containsAll(levels)) {
            throw new IllegalArgumentException("Unsupported revision-three control level");
        }
        if (guardianPartySizes.stream().anyMatch(size -> size != 3 && size != 7)) {
            throw new IllegalArgumentException("Guardian party size must be 3 or 7");
        }
        if (!mode.equals("DIAGNOSTIC")) {
            final boolean allMatchups = matchups.size() == V3Matchup.values().length
                && Set.copyOf(matchups).equals(Set.of(V3Matchup.values()));
            final boolean allLevels = levels.size() == Step12SimulationFixtures.CONTROL_LEVELS.size()
                && Set.copyOf(levels).equals(Set.copyOf(Step12SimulationFixtures.CONTROL_LEVELS));
            final boolean allGuardianSizes = guardianPartySizes.size() == 2
                && Set.copyOf(guardianPartySizes).equals(Set.of(3, 7));
            if (!allMatchups || !allLevels || !allGuardianSizes) {
                throw new IllegalArgumentException(
                    "V3 causal acceptance requires all 6 matchups × 5 levels and Guardian 3/7"
                );
            }
        }
    }

    static List<String> acceptanceCellDescriptors(
        long rootSeed,
        List<V3Matchup> matchups,
        List<Integer> levels,
        List<Integer> guardianPartySizes
    ) {
        final var descriptors = new ArrayList<String>();
        for (final var cell : cells(rootSeed, matchups, levels)) {
            for (final var world : FactorialWorld.values()) {
                descriptors.add("FACTORIAL|%s|%s|%d|%d".formatted(
                    cell.code(),
                    world.code(),
                    cell.level(),
                    cell.seed()
                ));
            }
        }
        for (final var cell : guardianCells(rootSeed, levels, guardianPartySizes)) {
            descriptors.add("GUARDIAN|%s|%d|%d|%d".formatted(
                cell.code(),
                cell.partySize(),
                cell.level(),
                cell.seed()
            ));
        }
        return List.copyOf(descriptors);
    }

    private static void rejectLegacyInputProperties() {
        final var legacy = List.of("inputManifest", "inputRoot", "inputFingerprint").stream()
            .filter(name -> System.getProperty(PREFIX + name) != null)
            .toList();
        if (!legacy.isEmpty()) {
            throw new IllegalArgumentException(
                "V3 acceptance input paths and fingerprints are fixed; remove properties " + legacy
            );
        }
    }

    private static List<CausalCell> cells(
        long rootSeed,
        List<V3Matchup> matchups,
        List<Integer> levels
    ) {
        final var result = new ArrayList<CausalCell>();
        for (final var matchup : matchups) {
            for (final int level : levels) {
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

    private static List<GuardianCell> guardianCells(
        long rootSeed,
        List<Integer> levels,
        List<Integer> partySizes
    ) {
        final var result = new ArrayList<GuardianCell>();
        for (final int partySize : partySizes) {
            for (final int level : levels) {
                final var code = "СТРАЖ_V3_ПРИЧИНА_N%d_L%d".formatted(partySize, level);
                result.add(new GuardianCell(code, partySize, level, cellSeed(rootSeed, code)));
            }
        }
        return List.copyOf(result);
    }

    private static CellResult runCell(CausalCell cell, int iterations, int maxRounds, String mode) {
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
        return new CellResult(
            cell,
            Map.copyOf(variants),
            effects,
            primary(cell, iterations, maxRounds, mode)
        );
    }

    private static PairedCombatRequest request(
        CausalCell cell,
        FactorialWorld world,
        int iterations,
        int maxRounds
    ) {
        final var disabled = world.disabled(cell.roles());
        return new PairedCombatRequest(
            "V3_CAUSAL_FACTORIAL",
            cell.code() + "_" + world.code(),
            cell.roles().evaluated().displayName(),
            cell.roles().opponent().displayName(),
            3,
            iterations,
            maxRounds,
            cell.seed(),
            () -> Step12SimulationFixtures.v3Teams(cell.matchup(), cell.level(), disabled)
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
            pValues[index] = Step12CausalStatistics.pairedSignFlipPValue(
                contrasts.get(index).differences(),
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

    private static PrimaryResult primary(CausalCell cell, int iterations, int maxRounds, String mode) {
        return switch (cell.matchup()) {
            case BRUISER_V3 -> bruiserPrimary(cell, iterations, maxRounds);
            case SKIRMISHER_V3 -> binaryPrimary(
                cell,
                iterations,
                index -> Step12V3CausalObservationRunner.skirmisherPair(
                    cell.level(), cell.seed(), index, maxRounds
                ),
                "первый отход в фактическом направлении отнимает следующую попытку и заряд Разрушителя",
                mode
            );
            case ASSASSIN_V3 -> assassinPrimary(cell, iterations, maxRounds);
            case RANGER_V3 -> naturalMeanPrimary(
                cell,
                iterations,
                maxRounds,
                NaturalPairObservation::rangerFarAttemptShare,
                "доля обычных попыток Дальнобойца с дистанции 3–4",
                mode
            );
            case BREAKER_V3 -> naturalMeanPrimary(
                cell,
                iterations,
                maxRounds,
                NaturalPairObservation::breakerDischargeScore,
                "доля начальных значений хотя бы с одним разрядом",
                mode
            );
            case TACTICIAN_V3 -> tacticianPrimary(cell, iterations, maxRounds, mode);
        };
    }

    private static PrimaryResult bruiserPrimary(CausalCell cell, int iterations, int maxRounds) {
        final var bruiserDamage = new double[iterations];
        final var bruiserAttempts = new double[iterations];
        final var guardianDamage = new double[iterations];
        final var guardianAttempts = new double[iterations];
        for (int index = 0; index < iterations; index++) {
            final var observation = Step12V3CausalObservationRunner.naturalPair(
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
            ) + interval(damageDifference) + String.format(Locale.ROOT, "; p=%.5f", damagePValue),
            Optional.empty(),
            Optional.empty()
        );
    }

    private static PrimaryResult assassinPrimary(CausalCell cell, int iterations, int maxRounds) {
        final var exact = exactBinaryPrimary(
            cell,
            iterations,
            index -> Step12V3CausalObservationRunner.assassinPair(cell.level(), cell.seed(), index),
            "доступ и единственный пакет открывают дальнюю цель; цель фиксируется ровно на два продолжения "
                + "без повторного пакета и окна"
        );
        final var naturalAccess = new double[iterations];
        for (int index = 0; index < iterations; index++) {
            naturalAccess[index] = Step12V3CausalObservationRunner.naturalPair(
                cell.matchup(), cell.level(), cell.seed(), index, maxRounds
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
            Optional.empty(),
            Optional.empty()
        );
    }

    private static PrimaryResult tacticianPrimary(
        CausalCell cell,
        int iterations,
        int maxRounds,
        String mode
    ) {
        final var applications = new double[iterations];
        for (int index = 0; index < iterations; index++) {
            applications[index] = Step12V3CausalObservationRunner.naturalPair(
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
        final TempoExactObservation exact = Step12V3CausalObservationRunner.tempoExact(
            cell.level(),
            streamSeed(cell.seed(), "tempo-exact")
        );
        final TempoBenchmarkObservation benchmark = Step12V3CausalObservationRunner.tempoBenchmark(
            cell.level(),
            streamSeed(cell.seed(), "tempo-benchmark"),
            10_000,
            100_000
        );
        return new PrimaryResult(
            estimate,
            thresholdAccepted(estimate, mode) && exact.accepted() && tempoBenchmarkAccepted(benchmark),
            "доля применений к названному Громиле: " + thresholdDescription(mode) + "; "
                + "при L=92 снимается ровно 331; потеря действий в длинном опыте (0; 15%]",
            Optional.of(exact),
            Optional.of(benchmark)
        );
    }

    private static PrimaryResult naturalMeanPrimary(
        CausalCell cell,
        int iterations,
        int maxRounds,
        java.util.function.ToDoubleFunction<NaturalPairObservation> extractor,
        String description,
        String mode
    ) {
        final var observations = new double[iterations];
        for (int index = 0; index < iterations; index++) {
            observations[index] = extractor.applyAsDouble(Step12V3CausalObservationRunner.naturalPair(
                cell.matchup(), cell.level(), cell.seed(), index, maxRounds
            ));
        }
        return thresholdPrimary(
            Step12CausalStatistics.mean(observations, streamSeed(cell.seed(), "primary")),
            description,
            mode
        );
    }

    private static PrimaryResult binaryPrimary(
        CausalCell cell,
        int iterations,
        java.util.function.IntToDoubleFunction observation,
        String description,
        String mode
    ) {
        final var observations = new double[iterations];
        for (int index = 0; index < iterations; index++) {
            observations[index] = observation.applyAsDouble(index);
        }
        return thresholdPrimary(
            Step12CausalStatistics.mean(observations, streamSeed(cell.seed(), "primary")),
            description,
            mode
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
        return new PrimaryResult(
            Step12CausalStatistics.mean(observations, streamSeed(cell.seed(), "primary")),
            accepted,
            description + "; требуется каждое управляемое состояние",
            Optional.empty(),
            Optional.empty()
        );
    }

    private static PrimaryResult thresholdPrimary(Estimate estimate, String description, String mode) {
        return new PrimaryResult(
            estimate,
            thresholdAccepted(estimate, mode),
            description + "; " + thresholdDescription(mode),
            Optional.empty(),
            Optional.empty()
        );
    }

    static boolean thresholdAccepted(Estimate estimate, String mode) {
        return mode.equals("PREFLIGHT")
            ? estimate.estimate() >= 0.75
            : estimate.confidenceInterval().lower() >= 0.75
                && estimate.confidenceInterval().width() <= 0.02 + 1e-12;
    }

    private static String thresholdDescription(String mode) {
        return mode.equals("PREFLIGHT")
            ? "точечная оценка ≥75%, интервал справочный"
            : "нижняя граница ≥75%, ширина ≤2 п.п.";
    }

    private static GuardianResult runGuardianCell(
        GuardianCell cell,
        int iterations,
        int maxRounds,
        String mode
    ) {
        final var useful = new double[iterations];
        final var lifetimeDamageReduction = new double[iterations];
        final var turnIncrease = new double[iterations];
        var capKept = true;
        for (int index = 0; index < iterations; index++) {
            final GuardianPairObservation observation = Step12V3CausalObservationRunner.guardianPair(
                cell.partySize(), cell.level(), cell.seed(), index, maxRounds
            );
            useful[index] = observation.usefulInterceptionScore();
            lifetimeDamageReduction[index] = observation.lifetimeWardDamageReduction();
            turnIncrease[index] = observation.wardTurnIncrease();
            capKept &= observation.interceptionLimitKept();
        }
        final var usefulEstimate = Step12CausalStatistics.mean(
            useful,
            streamSeed(cell.seed(), "guardian-useful")
        );
        final var lifetimeDamageEstimate = Step12CausalStatistics.mean(
            lifetimeDamageReduction,
            streamSeed(cell.seed(), "guardian-lifetime-damage")
        );
        final var turnEstimate = Step12CausalStatistics.mean(
            turnIncrease,
            streamSeed(cell.seed(), "guardian-turns")
        );
        final double turnPValue = Step12CausalStatistics.pairedSignFlipPValue(
            turnIncrease,
            streamSeed(cell.seed(), "guardian-turns-p")
        );
        final boolean accepted = guardianAccepted(
            usefulEstimate,
            turnEstimate,
            turnPValue,
            capKept,
            mode
        );
        return new GuardianResult(
            cell,
            usefulEstimate,
            lifetimeDamageEstimate,
            turnEstimate,
            turnPValue,
            capKept,
            accepted,
            "первая полезная возможность: " + thresholdDescription(mode)
                + "; действий союзника больше, предел соблюдён; "
                + "полный пожизненный урон только описательный"
        );
    }

    private static <T, R> List<R> runParallel(
        List<T> values,
        int workers,
        java.util.function.Function<T, R> operation
    ) {
        if (values.isEmpty()) {
            return List.of();
        }
        if (workers == 1 || values.size() == 1) {
            return values.stream().map(operation).toList();
        }
        final var pool = new ForkJoinPool(Math.min(workers, values.size()));
        try {
            return pool.submit(() -> values.parallelStream().map(operation).toList()).get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("V3 causal diagnosis was interrupted", exception);
        } catch (ExecutionException exception) {
            final var cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException("V3 causal diagnosis failed", cause);
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
            .append(configuration.mode().equals("FINAL")
                ? "# Итоговая причинная проверка третьей редакции шага 12\n\n"
                : "# Причинная диагностика третьей редакции шага 12\n\n")
            .append("Код: `").append(REVISION).append("`; режим: `")
            .append(configuration.mode()).append("`; корень: `")
            .append(configuration.rootSeed()).append("`; начальных значений на строку: `")
            .append(configuration.iterations()).append("`; два порядка сторон. ")
            .append(acceptanceDescription(configuration))
            .append("## Разложение доли побед\n\n")
            .append("| Ячейка V3 | `++` | `-+` | `+-` | `--` | Первичный показатель | Маршрут |\n")
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
            .append("| Ячейка V3 | Сравнение | Разность и 95-процентный интервал | p | p Холма |\n")
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
        output.append("\n## Срыв темпа V3\n\n")
            .append("| Ячейка V3 | L | Снятие в двух направлениях | Действия Громилы с умением | ")
            .append("Контроль | Потеря | Итог |\n")
            .append("|---|---:|---:|---:|---:|---:|---|\n");
        for (final var result : results) {
            if (result.primary().tempoExact().isPresent() && result.primary().tempoBenchmark().isPresent()) {
                final var exact = result.primary().tempoExact().orElseThrow();
                final var benchmark = result.primary().tempoBenchmark().orElseThrow();
                output.append(String.format(
                    Locale.ROOT,
                    "| %s | %d/%d | %d/%d | %d | %d | %.3f%% | %s |%n",
                    result.cell().code(),
                    exact.firstImpactStrength(),
                    exact.secondImpactStrength(),
                    exact.firstRemoved(),
                    exact.secondRemoved(),
                    benchmark.activeBruiserTurns(),
                    benchmark.controlBruiserTurns(),
                    benchmark.relativeLoss() * 100,
                    mark(exact.accepted() && tempoBenchmarkAccepted(benchmark))
                ));
            }
        }
        output.append("\n## Страж V3\n\n")
            .append("| Ячейка | Первая полезная возможность | Пожизненное снижение урона (описательно) | ")
            .append("Прибавка действий | p | Предел | Итог | Маршрут |\n")
            .append("|---|---:|---:|---:|---:|---|---|---|\n");
        for (final var result : guardianResults) {
            output.append("| ").append(result.cell().code()).append(" | ")
                .append(percentEstimate(result.useful())).append(" | ")
                .append(numberEstimate(result.lifetimeDamageReduction())).append(" | ")
                .append(numberEstimate(result.turnIncrease())).append(" | ")
                .append(String.format(Locale.ROOT, "%.5f", result.turnPValue())).append(" | ")
                .append(mark(result.capKept())).append(" | ")
                .append(mark(result.accepted())).append(" | ")
                .append(result.accepted() ? "не требуется" : "шаг 6").append(" |\n");
        }
        output.append("\n## Итоговый маршрут ролей V3\n\n")
            .append("| Роль | Маршруты уровней | Итог |\n")
            .append("|---|---|---|\n");
        for (final var matchup : results.stream().map(result -> result.cell().matchup()).distinct().toList()) {
            final var roleResults = results.stream()
                .filter(result -> result.cell().matchup() == matchup)
                .toList();
            output.append("| ").append(rolePair(matchup).evaluated().displayName()).append(" | ")
                .append(roleResults.stream()
                    .map(result -> "+" + result.cell().level() + ": " + route(result))
                    .collect(java.util.stream.Collectors.joining("; ")))
                .append(" | ").append(roleRoute(roleResults)).append(" |\n");
        }
        if (!guardianResults.isEmpty()) {
            output.append("| Страж V3 | ")
                .append(guardianResults.stream()
                    .map(result -> result.cell().partySize() + "×" + result.cell().partySize()
                        + " +" + result.cell().level() + ": "
                        + (result.accepted() ? "не требуется" : "шаг 6"))
                    .collect(java.util.stream.Collectors.joining("; ")))
                .append(" | ")
                .append(guardianResults.stream().allMatch(GuardianResult::accepted) ? "не требуется" : "шаг 6")
                .append(" |\n");
        }
        output.append("\n## Определения первичных показателей V3\n\n");
        for (final var result : results) {
            output.append("- `").append(result.cell().code()).append("`: ")
                .append(result.primary().description()).append("\n");
        }
        return output.toString();
    }

    private static String acceptanceDescription(Configuration configuration) {
        if (configuration.acceptanceProtocol().equals("V4")) {
            return configuration.mode().equals("FINAL")
                ? "Это итоговая причинная выборка кандидата V3 по протоколу приёмки V4.\n\n"
                : "Это предварительный причинный прогон кандидата V3 по протоколу приёмки V4, "
                    + "не итоговая выборка `" + V4_FINAL_ROOT + "`.\n\n";
        }
        return configuration.mode().equals("FINAL")
            ? "Это итоговая причинная выборка V3.\n\n"
            : "Это предварительный причинный прогон V3, не итоговая выборка `"
                + FINAL_ROOT + "`.\n\n";
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
        return result.cell().matchup() == V3Matchup.BRUISER_V3
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

    static boolean guardianAccepted(
        Estimate useful,
        Estimate turnIncrease,
        double turnPValue,
        boolean capKept,
        String mode
    ) {
        return thresholdAccepted(useful, mode)
            && turnIncrease.confidenceInterval().lower() > 0
            && turnPValue <= 0.05
            && capKept;
    }

    static boolean tempoBenchmarkAccepted(TempoBenchmarkObservation benchmark) {
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
        final var fullPosition = outcomePosition(
            result.variants().get(FactorialWorld.BOTH_ON),
            result.cell().level()
        );
        final var baselinePosition = outcomePosition(
            result.variants().get(FactorialWorld.BOTH_OFF),
            result.cell().level()
        );
        if (fullPosition == OutcomePosition.ACCEPTED) {
            return "не требуется";
        }
        final var total = effect(result, "оба умения вместе");
        final var interaction = effect(result, "взаимодействие A и B");
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

    private static EffectResult effect(CellResult result, String name) {
        return result.effects().stream()
            .filter(effect -> effect.name().equals(name))
            .findFirst()
            .orElseThrow();
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
        final var requiredRoutes = results.stream().map(Step12V3CausalSimulator::route).distinct().toList();
        return requiredRoutes.size() == 1 ? requiredRoutes.getFirst() : "смешанный";
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
            return result.lower() > 0.50
                ? OutcomePosition.ACCEPTED
                : result.upper() <= 0.50 ? OutcomePosition.BELOW : OutcomePosition.UNCERTAIN;
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

    private static V3RolePair rolePair(V3Matchup matchup) {
        return switch (matchup) {
            case BRUISER_V3 -> new V3RolePair(V3Build.BRUISER_BLUNT, V3Build.GUARDIAN_BLUNT);
            case SKIRMISHER_V3 -> new V3RolePair(V3Build.SKIRMISHER_SLASH, V3Build.BREAKER_CLOSE_SLASH);
            case ASSASSIN_V3 -> new V3RolePair(V3Build.ASSASSIN_PIERCE, V3Build.RANGER_PIERCE);
            case RANGER_V3 -> new V3RolePair(V3Build.RANGER_MAGICAL, V3Build.BRUISER_MAGICAL_CLOTH);
            case BREAKER_V3 -> new V3RolePair(V3Build.BREAKER_MAGICAL_LEATHER, V3Build.BRUISER_MAGICAL_LEATHER);
            case TACTICIAN_V3 -> new V3RolePair(V3Build.TACTICIAN_MAGICAL, V3Build.BRUISER_MAGICAL_CLOTH);
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

        private Set<V3Build> disabled(V3RolePair roles) {
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

    record Configuration(
        String mode,
        long rootSeed,
        int iterations,
        int maxRounds,
        int workers,
        boolean enforce,
        String output,
        String acceptanceProtocol
    ) {
        Configuration(
            String mode,
            long rootSeed,
            int iterations,
            int maxRounds,
            int workers,
            boolean enforce,
            String output
        ) {
            this(mode, rootSeed, iterations, maxRounds, workers, enforce, output, "V3");
        }

        Configuration {
            Objects.requireNonNull(mode, "mode");
            Objects.requireNonNull(output, "output");
            Objects.requireNonNull(acceptanceProtocol, "acceptanceProtocol");
        }
    }

    record DiagnosticRun(
        List<CellResult> results,
        List<GuardianResult> guardianResults,
        String markdown
    ) {
        DiagnosticRun {
            results = List.copyOf(results);
            guardianResults = List.copyOf(guardianResults);
            Objects.requireNonNull(markdown, "markdown");
        }
    }

    record CausalCell(
        String code,
        V3Matchup matchup,
        int level,
        long seed,
        V3RolePair roles
    ) {
    }

    record GuardianCell(String code, int partySize, int level, long seed) {
    }

    record V3RolePair(V3Build evaluated, V3Build opponent) {
    }

    record VariantResult(double winRate, double lower, double upper, double firstSideWinRate) {
    }

    private record EffectContrast(String name, double[] differences) {
    }

    record EffectResult(String name, Estimate estimate, double pValue, double holmPValue) {
    }

    record PrimaryResult(
        Estimate estimate,
        boolean accepted,
        String description,
        Optional<TempoExactObservation> tempoExact,
        Optional<TempoBenchmarkObservation> tempoBenchmark
    ) {
        PrimaryResult {
            Objects.requireNonNull(tempoExact, "tempoExact");
            Objects.requireNonNull(tempoBenchmark, "tempoBenchmark");
        }
    }

    record CellResult(
        CausalCell cell,
        Map<FactorialWorld, VariantResult> variants,
        List<EffectResult> effects,
        PrimaryResult primary
    ) {
    }

    record GuardianResult(
        GuardianCell cell,
        Estimate useful,
        Estimate lifetimeDamageReduction,
        Estimate turnIncrease,
        double turnPValue,
        boolean capKept,
        boolean accepted,
        String description
    ) {
    }
}
