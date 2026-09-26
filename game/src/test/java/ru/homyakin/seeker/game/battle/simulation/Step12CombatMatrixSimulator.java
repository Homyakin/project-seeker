package ru.homyakin.seeker.game.battle.simulation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import ru.homyakin.seeker.game.battle.simulation.CombatSimulationReport.ConfidenceInterval;
import ru.homyakin.seeker.game.battle.simulation.PairedCombatSimulator.PairedCombatReport;
import ru.homyakin.seeker.game.battle.simulation.PairedCombatSimulator.PairedCombatRequest;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.RoleBuild;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.V2Matchup;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.V3Matchup;

/** Property-gated entry point for the mirror and favorable-matchup layer of the step 12 matrix. */
@EnabledIfSystemProperty(named = "step12.matrix.enabled", matches = "true")
class Step12CombatMatrixSimulator {
    private static final String PREFIX = "step12.matrix.";
    private static final long DEFAULT_ROOT_SEED = 20_260_912L;
    private static final long FINAL_V2_ROOT_SEED = 2_026_091_802L;
    private static final long PREFLIGHT_V3_ROOT_SEED = 2_026_092_201L;
    private static final long PREFLIGHT_V3_CAUSAL_ROOT_SEED = 2_026_092_202L;
    private static final long FINAL_V3_ROOT_SEED = 2_026_092_301L;
    private static final long PREFLIGHT_V4_ROOT_SEED = 2_026_092_203L;
    private static final long PREFLIGHT_V4_CAUSAL_ROOT_SEED = 2_026_092_204L;
    private static final long FINAL_V4_ROOT_SEED = 2_026_092_302L;
    private static final int PREFLIGHT_ITERATIONS = 2_000;
    private static final int FINAL_ITERATIONS = 10_000;
    private static final int REQUIRED_MAX_ROUNDS = 10_000;
    private static final int EXPECTED_MATRIX_CELLS = 180;
    private static final int EXPECTED_MIRROR_CELLS = 150;
    private static final int EXPECTED_FAVORABLE_CELLS = 30;
    private static final List<String> FILTER_PROPERTIES = List.of(
        "family", "levels", "partySizes", "scenario"
    );
    private static final long FNV_OFFSET_BASIS = 0xcbf29ce484222325L;
    private static final long FNV_PRIME = 0x100000001b3L;
    private static final List<LegacyFavorableMatchup> LEGACY_FAVORABLE_MATCHUPS = List.of(
        new LegacyFavorableMatchup("FAVORABLE_BRUISER", RoleBuild.BRUISER, RoleBuild.GUARDIAN),
        new LegacyFavorableMatchup(
            "FAVORABLE_SKIRMISHER", RoleBuild.SKIRMISHER, RoleBuild.BREAKER_PHYSICAL
        ),
        new LegacyFavorableMatchup(
            "FAVORABLE_ASSASSIN", RoleBuild.ASSASSIN, RoleBuild.RANGER_PHYSICAL
        ),
        new LegacyFavorableMatchup(
            "FAVORABLE_RANGER", RoleBuild.RANGER_MAGICAL, RoleBuild.BRUISER
        ),
        new LegacyFavorableMatchup(
            "FAVORABLE_BREAKER", RoleBuild.BREAKER_MAGICAL, RoleBuild.BRUISER
        ),
        new LegacyFavorableMatchup(
            "FAVORABLE_TACTICIAN", RoleBuild.TACTICIAN_PHYSICAL, RoleBuild.BRUISER
        )
    );

    @Test
    void runConfiguredMatrix() throws Exception {
        final int iterations = intProperty("iterations", 1_000);
        final int maxRounds = intProperty("maxRounds", 10_000);
        final long rootSeed = longProperty("seed", DEFAULT_ROOT_SEED);
        final var configuration = new MatrixRunConfiguration(
            property("revision", "V1").toUpperCase(Locale.ROOT),
            property("mode", "DIAGNOSTIC").toUpperCase(Locale.ROOT),
            iterations,
            maxRounds,
            rootSeed,
            booleanProperty("enforce", false),
            presentFilterProperties(),
            property("acceptance", "V3").toUpperCase(Locale.ROOT)
        );
        validateRunConfiguration(configuration);
        final var family = MatrixFamily.valueOf(property("family", "ALL").toUpperCase(Locale.ROOT));
        final var levels = intSetProperty("levels", Step12SimulationFixtures.CONTROL_LEVELS);
        final var partySizes = intSetProperty("partySizes", Step12SimulationFixtures.MIRROR_PARTY_SIZES);
        final var scenario = property("scenario", "").trim();
        final boolean enforce = configuration.enforce();
        final boolean details = booleanProperty("details", false);
        final int workers = intProperty(
            "workers",
            Math.min(4, Runtime.getRuntime().availableProcessors())
        );
        final var cells = cells(rootSeed, configuration.revision()).stream()
            .filter(cell -> family == MatrixFamily.ALL || cell.family() == family)
            .filter(cell -> levels.contains(cell.enhanceLevel()))
            .filter(cell -> cell.family() != MatrixFamily.MIRROR || partySizes.contains(cell.partySize()))
            .filter(cell -> scenario.isEmpty() || cell.code().equalsIgnoreCase(scenario))
            .toList();
        if (cells.isEmpty()) {
            throw new IllegalArgumentException("Step 12 matrix filters selected no cells");
        }
        if (configuration.acceptanceRun()) {
            if (configuration.revision().equals("V2")) {
                validateFinalCells(cells);
            } else {
                validateFinalV3Cells(cells);
            }
        }
        final Step12V3AcceptanceLedger v3Ledger;
        final Step12V3AcceptanceLedger.Attempt v3Attempt;
        final Step12V4AcceptanceLedger v4Ledger;
        final Step12V4AcceptanceLedger.Attempt v4Attempt;
        if (configuration.v3AcceptanceRun()) {
            rejectLegacyInputProperties();
            v3Ledger = Step12V3AcceptanceLedger.fromTestClasses(Step12CombatMatrixSimulator.class);
            final var kind = configuration.preflightRun()
                ? Step12V3AcceptanceLedger.RunKind.OUTCOME_PREFLIGHT
                : Step12V3AcceptanceLedger.RunKind.OUTCOME_FINAL;
            v3Attempt = beginV3Acceptance(
                v3Ledger,
                kind,
                configuration,
                acceptanceCellDescriptors(cells),
                System.getProperty(PREFIX + "output"),
                workers
            );
            v4Ledger = null;
            v4Attempt = null;
        } else if (configuration.v4AcceptanceRun()) {
            rejectLegacyInputProperties();
            v4Ledger = Step12V4AcceptanceLedger.fromTestClasses(Step12CombatMatrixSimulator.class);
            final var kind = configuration.preflightRun()
                ? Step12V4AcceptanceLedger.RunKind.OUTCOME_PREFLIGHT
                : Step12V4AcceptanceLedger.RunKind.OUTCOME_FINAL;
            v4Attempt = beginV4Acceptance(
                v4Ledger,
                kind,
                configuration,
                acceptanceCellDescriptors(cells),
                System.getProperty(PREFIX + "output"),
                workers
            );
            v3Ledger = null;
            v3Attempt = null;
        } else {
            v3Ledger = null;
            v3Attempt = null;
            v4Ledger = null;
            v4Attempt = null;
        }

        final var results = runCells(cells, iterations, maxRounds, workers);

        final var markdown = markdown(
            results,
            iterations,
            maxRounds,
            rootSeed,
            configuration.mode(),
            configuration.acceptanceProtocol(),
            details
        );
        final Path output;
        if (v3Attempt != null) {
            output = v3Attempt.report();
            v3Ledger.writeReport(v3Attempt, markdown);
        } else if (v4Attempt != null) {
            output = v4Attempt.report();
            v4Ledger.writeReport(v4Attempt, markdown);
        } else {
            output = Path.of(property("output", "target/step12-combat-matrix-report.md"));
            if (output.getParent() != null) {
                Files.createDirectories(output.getParent());
            }
            Files.writeString(output, markdown);
        }
        System.out.println(markdown);
        System.out.println("Report: " + output.toAbsolutePath());

        if (enforce) {
            final var failures = results.stream()
                .filter(result -> configuration.preflightRun()
                    ? !preflightOutcomeAccepted(
                        configuration.acceptanceProtocol(),
                        result.cell().family(),
                        result.cell().enhanceLevel(),
                        result.report().evaluatedWinRate(),
                        result.report().firstSideWinRate()
                    )
                    : !result.decision().outcomeLayerAccepted())
                .map(result -> configuration.preflightRun()
                    ? result.cell().code() + ": точечная оценка исхода не прошла предварительный коридор"
                    : result.cell().code() + ": " + result.decision().description())
                .toList();
            Assertions.assertTrue(failures.isEmpty(), () -> String.join(System.lineSeparator(), failures));
        }
        if (v3Attempt != null) {
            v3Ledger.complete(v3Attempt);
        } else if (v4Attempt != null) {
            v4Ledger.complete(v4Attempt);
        }
    }

    static List<String> acceptanceCellDescriptors(List<MatrixCell> cells) {
        return cells.stream()
            .map(cell -> "%s|%s|%d|%d|%d|%s".formatted(
                cell.code(),
                cell.family(),
                cell.partySize(),
                cell.enhanceLevel(),
                cell.seed(),
                cell.fixture().getClass().getSimpleName()
            ))
            .toList();
    }

    static Step12V3AcceptanceLedger.Attempt beginV3Acceptance(
        Step12V3AcceptanceLedger ledger,
        Step12V3AcceptanceLedger.RunKind kind,
        MatrixRunConfiguration configuration,
        List<String> cellDescriptors,
        String output,
        int workers
    ) {
        validateWorkers(workers);
        return ledger.begin(
            kind,
            configuration.rootSeed(),
            configuration.iterations(),
            configuration.maxRounds(),
            cellDescriptors,
            output
        );
    }

    static Step12V4AcceptanceLedger.Attempt beginV4Acceptance(
        Step12V4AcceptanceLedger ledger,
        Step12V4AcceptanceLedger.RunKind kind,
        MatrixRunConfiguration configuration,
        List<String> cellDescriptors,
        String output,
        int workers
    ) {
        validateWorkers(workers);
        return ledger.begin(
            kind,
            configuration.rootSeed(),
            configuration.iterations(),
            configuration.maxRounds(),
            cellDescriptors,
            output
        );
    }

    private static void rejectLegacyInputProperties() {
        final var legacy = List.of("inputManifest", "inputRoot", "inputFingerprint").stream()
            .filter(name -> System.getProperty(PREFIX + name) != null)
            .toList();
        if (!legacy.isEmpty()) {
            throw new IllegalArgumentException(
                "Step 12 acceptance input paths and fingerprints are fixed; remove properties " + legacy
            );
        }
    }

    static List<CellResult> runCells(
        List<MatrixCell> cells,
        int iterations,
        int maxRounds,
        int workers
    ) {
        validateWorkers(workers);
        if (workers == 1 || cells.size() == 1) {
            return cells.stream()
                .map(cell -> runCell(cell, iterations, maxRounds))
                .toList();
        }

        final var pool = new ForkJoinPool(Math.min(workers, cells.size()));
        try {
            return pool.submit(() -> cells.parallelStream()
                .map(cell -> runCell(cell, iterations, maxRounds))
                .toList()
            ).get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Step 12 matrix execution was interrupted", exception);
        } catch (ExecutionException exception) {
            final var cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException("Step 12 matrix execution failed", cause);
        } finally {
            pool.shutdown();
        }
    }

    private static void validateWorkers(int workers) {
        if (workers <= 0) {
            throw new IllegalArgumentException("workers must be positive");
        }
    }

    private static CellResult runCell(MatrixCell cell, int iterations, int maxRounds) {
        final var rawReport = new PairedCombatSimulator().run(cell.request(iterations, maxRounds));
        final var cellDecision = decision(cell, rawReport);
        return new CellResult(cell, rawReport.withoutRawOutcomes(), cellDecision);
    }

    static List<MatrixCell> cells(long rootSeed) {
        return cells(rootSeed, "V2");
    }

    static List<MatrixCell> cells(long rootSeed, String revision) {
        if (!Set.of("V1", "V2", "V3").contains(revision)) {
            throw new IllegalArgumentException("Unknown step 12 matrix revision: " + revision);
        }
        final var result = new ArrayList<MatrixCell>();
        for (final var build : RoleBuild.values()) {
            for (final var partySize : Step12SimulationFixtures.MIRROR_PARTY_SIZES) {
                for (final var level : Step12SimulationFixtures.CONTROL_LEVELS) {
                    final var code = "MIRROR_%s_N%d_L%d".formatted(build.name(), partySize, level);
                    result.add(new MatrixCell(
                        code,
                        MatrixFamily.MIRROR,
                        new MirrorFixture(build, revision.equals("V3")),
                        partySize,
                        level,
                        cellSeed(rootSeed, code),
                        CausalGate.NOT_REQUIRED
                    ));
                }
            }
        }
        if (revision.equals("V1")) {
            addLegacyFavorableCells(result, rootSeed);
        } else if (revision.equals("V2")) {
            addV2FavorableCells(result, rootSeed);
        } else {
            addV3FavorableCells(result, rootSeed);
        }
        return List.copyOf(result);
    }

    private static void addLegacyFavorableCells(List<MatrixCell> result, long rootSeed) {
        for (final var matchup : LEGACY_FAVORABLE_MATCHUPS) {
            for (final var level : Step12SimulationFixtures.CONTROL_LEVELS) {
                final var code = matchup.code() + "_N3_L" + level;
                result.add(new MatrixCell(
                    code,
                    MatrixFamily.FAVORABLE,
                    new LegacyFavorableFixture(matchup.evaluated(), matchup.opponents()),
                    3,
                    level,
                    cellSeed(rootSeed, code),
                    CausalGate.NOT_REGISTERED
                ));
            }
        }
    }

    private static void addV2FavorableCells(List<MatrixCell> result, long rootSeed) {
        for (final var matchup : V2Matchup.values()) {
            for (final var level : Step12SimulationFixtures.CONTROL_LEVELS) {
                final var code = matchup.code() + "_N3_L" + level;
                result.add(new MatrixCell(
                    code,
                    MatrixFamily.FAVORABLE,
                    new V2FavorableFixture(matchup),
                    3,
                    level,
                    cellSeed(rootSeed, code),
                    CausalGate.forMatchup(matchup)
                ));
            }
        }
    }

    private static void addV3FavorableCells(List<MatrixCell> result, long rootSeed) {
        for (final var matchup : V3Matchup.values()) {
            for (final int level : Step12SimulationFixtures.CONTROL_LEVELS) {
                final var code = matchup.code() + "_N3_L" + level;
                result.add(new MatrixCell(
                    code,
                    MatrixFamily.FAVORABLE,
                    new V3FavorableFixture(matchup),
                    3,
                    level,
                    cellSeed(rootSeed, code),
                    CausalGate.forMatchup(matchup)
                ));
            }
        }
    }

    static long cellSeed(long rootSeed, String cellCode) {
        var hash = FNV_OFFSET_BASIS;
        for (int i = 0; i < cellCode.length(); i++) {
            hash ^= cellCode.charAt(i);
            hash *= FNV_PRIME;
        }
        var mixed = rootSeed ^ hash;
        mixed = (mixed ^ mixed >>> 30) * 0xbf58476d1ce4e5b9L;
        mixed = (mixed ^ mixed >>> 27) * 0x94d049bb133111ebL;
        return mixed ^ mixed >>> 31;
    }

    static void validateRunConfiguration(MatrixRunConfiguration configuration) {
        if (!Set.of("V1", "V2", "V3").contains(configuration.revision())) {
            throw new IllegalArgumentException("Unknown step 12 matrix revision: " + configuration.revision());
        }
        if (!Set.of("DIAGNOSTIC", "PREFLIGHT", "FINAL").contains(configuration.mode())) {
            throw new IllegalArgumentException("Unknown step 12 matrix mode: " + configuration.mode());
        }
        if (!Set.of("V3", "V4").contains(configuration.acceptanceProtocol())) {
            throw new IllegalArgumentException(
                "Unknown step 12 acceptance protocol: " + configuration.acceptanceProtocol()
            );
        }
        if (configuration.acceptanceProtocol().equals("V4") && !configuration.revision().equals("V3")) {
            throw new IllegalArgumentException("Acceptance protocol V4 requires gameplay revision V3");
        }
        if (configuration.mode().equals("DIAGNOSTIC")
            && Set.of(
                FINAL_V2_ROOT_SEED,
                PREFLIGHT_V3_ROOT_SEED,
                PREFLIGHT_V3_CAUSAL_ROOT_SEED,
                FINAL_V3_ROOT_SEED,
                PREFLIGHT_V4_ROOT_SEED,
                PREFLIGHT_V4_CAUSAL_ROOT_SEED,
                FINAL_V4_ROOT_SEED
            ).contains(configuration.rootSeed())) {
            throw new IllegalArgumentException("Acceptance root seeds are reserved");
        }
        if (configuration.mode().equals("PREFLIGHT") && !configuration.revision().equals("V3")) {
            throw new IllegalArgumentException("PREFLIGHT mode requires revision V3");
        }
        if (configuration.mode().equals("FINAL")
            && !Set.of("V2", "V3").contains(configuration.revision())) {
            throw new IllegalArgumentException("FINAL mode requires revision V2 or V3");
        }
        if (!configuration.acceptanceRun()) {
            return;
        }

        final var violations = new ArrayList<String>();
        final long expectedRootSeed;
        final int expectedIterations;
        if (configuration.preflightRun()) {
            expectedRootSeed = configuration.acceptanceProtocol().equals("V4")
                ? PREFLIGHT_V4_ROOT_SEED
                : PREFLIGHT_V3_ROOT_SEED;
            expectedIterations = PREFLIGHT_ITERATIONS;
        } else {
            expectedRootSeed = configuration.revision().equals("V2")
                ? FINAL_V2_ROOT_SEED
                : configuration.acceptanceProtocol().equals("V4")
                    ? FINAL_V4_ROOT_SEED
                    : FINAL_V3_ROOT_SEED;
            expectedIterations = FINAL_ITERATIONS;
        }
        if (configuration.iterations() != expectedIterations) {
            violations.add("iterations must equal " + expectedIterations);
        }
        if (configuration.maxRounds() != REQUIRED_MAX_ROUNDS) {
            violations.add("maxRounds must equal " + REQUIRED_MAX_ROUNDS);
        }
        if (configuration.rootSeed() != expectedRootSeed) {
            violations.add("seed must equal " + expectedRootSeed);
        }
        if (!configuration.enforce()) {
            violations.add("enforce must be true");
        }
        if (!configuration.presentFilters().isEmpty()) {
            violations.add("filters are forbidden: " + configuration.presentFilters());
        }
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException(
                "Invalid %s %s configuration: %s".formatted(
                    configuration.mode(),
                    configuration.revision(),
                    String.join("; ", violations)
                )
            );
        }
    }

    static void validateFinalCells(List<MatrixCell> cells) {
        final long mirrorCells = cells.stream().filter(cell -> cell.family() == MatrixFamily.MIRROR).count();
        final var favorable = cells.stream()
            .filter(cell -> cell.family() == MatrixFamily.FAVORABLE)
            .toList();
        final var violations = new ArrayList<String>();
        if (cells.size() != EXPECTED_MATRIX_CELLS) {
            violations.add("expected " + EXPECTED_MATRIX_CELLS + " cells, got " + cells.size());
        }
        if (mirrorCells != EXPECTED_MIRROR_CELLS) {
            violations.add("expected " + EXPECTED_MIRROR_CELLS + " mirror cells, got " + mirrorCells);
        }
        if (favorable.size() != EXPECTED_FAVORABLE_CELLS) {
            violations.add("expected " + EXPECTED_FAVORABLE_CELLS + " favorable cells, got " + favorable.size());
        }
        if (cells.stream().map(MatrixCell::code).distinct().count() != cells.size()) {
            violations.add("cell codes must be unique");
        }
        if (cells.stream().map(MatrixCell::seed).distinct().count() != cells.size()) {
            violations.add("cell seeds must be unique");
        }
        if (favorable.stream().anyMatch(cell -> !cell.code().contains("_V2_"))) {
            violations.add("every favorable cell must use a V2 code");
        }
        if (cells.stream()
            .filter(cell -> cell.family() == MatrixFamily.MIRROR)
            .anyMatch(cell -> !(cell.fixture() instanceof MirrorFixture))) {
            violations.add("every mirror cell must use a RoleBuild mirror fixture");
        }
        if (favorable.stream().anyMatch(cell -> !(cell.fixture() instanceof V2FavorableFixture))) {
            violations.add("every favorable cell must use a V2 matchup fixture");
        }
        if (favorable.stream().anyMatch(cell -> !cell.causalGate().separateRequired())) {
            violations.add("every favorable cell must name a separate causal gate");
        }
        final var expectedMatchups = Set.of(V2Matchup.values());
        final var actualMatchups = favorable.stream()
            .filter(cell -> cell.fixture() instanceof V2FavorableFixture)
            .map(cell -> ((V2FavorableFixture) cell.fixture()).matchup())
            .collect(Collectors.toUnmodifiableSet());
        if (!actualMatchups.equals(expectedMatchups)) {
            violations.add("expected all V2 matchups, got " + actualMatchups);
        }
        for (final var matchup : V2Matchup.values()) {
            for (final var level : Step12SimulationFixtures.CONTROL_LEVELS) {
                final long count = favorable.stream().filter(cell ->
                    cell.fixture() instanceof V2FavorableFixture fixture
                        && fixture.matchup() == matchup
                        && cell.enhanceLevel() == level
                ).count();
                if (count != 1) {
                    violations.add("expected one cell for " + matchup + " at +" + level + ", got " + count);
                }
            }
        }
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException("Invalid FINAL V2 cell set: " + String.join("; ", violations));
        }
    }

    static void validateFinalV3Cells(List<MatrixCell> cells) {
        final long mirrorCells = cells.stream().filter(cell -> cell.family() == MatrixFamily.MIRROR).count();
        final var favorable = cells.stream()
            .filter(cell -> cell.family() == MatrixFamily.FAVORABLE)
            .toList();
        final var violations = new ArrayList<String>();
        if (cells.size() != EXPECTED_MATRIX_CELLS) {
            violations.add("expected " + EXPECTED_MATRIX_CELLS + " cells, got " + cells.size());
        }
        if (mirrorCells != EXPECTED_MIRROR_CELLS) {
            violations.add("expected " + EXPECTED_MIRROR_CELLS + " mirror cells, got " + mirrorCells);
        }
        if (favorable.size() != EXPECTED_FAVORABLE_CELLS) {
            violations.add("expected " + EXPECTED_FAVORABLE_CELLS + " favorable cells, got " + favorable.size());
        }
        if (cells.stream().map(MatrixCell::code).distinct().count() != cells.size()) {
            violations.add("cell codes must be unique");
        }
        if (cells.stream().map(MatrixCell::seed).distinct().count() != cells.size()) {
            violations.add("cell seeds must be unique");
        }
        if (favorable.stream().anyMatch(cell -> !cell.code().contains("_V3_"))) {
            violations.add("every favorable cell must use a V3 code");
        }
        if (cells.stream()
            .filter(cell -> cell.family() == MatrixFamily.MIRROR)
            .anyMatch(cell -> !(cell.fixture() instanceof MirrorFixture))) {
            violations.add("every mirror cell must use a RoleBuild mirror fixture");
        }
        if (favorable.stream().anyMatch(cell -> !(cell.fixture() instanceof V3FavorableFixture))) {
            violations.add("every favorable cell must use a V3 matchup fixture");
        }
        if (favorable.stream().anyMatch(cell -> !cell.causalGate().separateRequired())) {
            violations.add("every favorable cell must name a separate causal gate");
        }
        final var actualMatchups = favorable.stream()
            .filter(cell -> cell.fixture() instanceof V3FavorableFixture)
            .map(cell -> ((V3FavorableFixture) cell.fixture()).matchup())
            .collect(Collectors.toUnmodifiableSet());
        if (!actualMatchups.equals(Set.of(V3Matchup.values()))) {
            violations.add("expected all V3 matchups, got " + actualMatchups);
        }
        for (final var matchup : V3Matchup.values()) {
            for (final int level : Step12SimulationFixtures.CONTROL_LEVELS) {
                final long count = favorable.stream().filter(cell ->
                    cell.fixture() instanceof V3FavorableFixture fixture
                        && fixture.matchup() == matchup
                        && cell.enhanceLevel() == level
                ).count();
                if (count != 1) {
                    violations.add("expected one cell for " + matchup + " at +" + level + ", got " + count);
                }
            }
        }
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException("Invalid FINAL V3 cell set: " + String.join("; ", violations));
        }
    }

    static Decision decision(MatrixCell cell, PairedCombatReport report) {
        final var interval = report.evaluatedWinRate95();
        final boolean outcomeAccepted = outcomeAccepted(
            cell.family(),
            cell.enhanceLevel(),
            report.evaluatedWinRate(),
            report.firstSideWinRate(),
            interval
        );
        final String expected;
        if (cell.family() == MatrixFamily.MIRROR) {
            expected = "победы и первая сторона в пределах 47–53%";
        } else if (Set.of(0, 10, 20).contains(cell.enhanceLevel())) {
            expected = "весь парный 95% ДИ побед проверяемой команды внутри 55–65%";
        } else {
            expected = "нижняя граница парного 95% ДИ побед проверяемой команды выше 50%";
        }
        final double intervalWidth = interval.upper() - interval.lower();
        final boolean precisionAccepted = intervalWidth <= 0.02 + 1e-12;
        final var description = ("%s; ширина парного 95%% ДИ %.2f п.п. (нужно ≤2.00); "
            + "отдельные причинные ворота: %s").formatted(
                expected,
                intervalWidth * 100,
                cell.causalGate().description()
            );
        return new Decision(
            outcomeAccepted,
            precisionAccepted,
            cell.causalGate().description(),
            description
        );
    }

    static boolean outcomeAccepted(
        MatrixFamily family,
        int enhanceLevel,
        double winRate,
        double firstSideWinRate,
        ConfidenceInterval interval
    ) {
        if (family == MatrixFamily.MIRROR) {
            return between(winRate, 0.47, 0.53) && between(firstSideWinRate, 0.47, 0.53);
        }
        if (Set.of(0, 10, 20).contains(enhanceLevel)) {
            return interval.lower() >= 0.55 && interval.upper() <= 0.65;
        }
        return interval.lower() > 0.5;
    }

    static boolean preflightOutcomeAccepted(
        MatrixFamily family,
        int enhanceLevel,
        double winRate,
        double firstSideWinRate
    ) {
        if (family == MatrixFamily.MIRROR) {
            return between(winRate, 0.47, 0.53) && between(firstSideWinRate, 0.47, 0.53);
        }
        if (Set.of(0, 10, 20).contains(enhanceLevel)) {
            return between(winRate, 0.55, 0.65);
        }
        return winRate > 0.5;
    }

    static boolean v4PreflightOutcomeAccepted(
        MatrixFamily family,
        int enhanceLevel,
        double winRate,
        double firstSideWinRate
    ) {
        if (family == MatrixFamily.MIRROR) {
            return between(winRate, 0.45, 0.55) && between(firstSideWinRate, 0.45, 0.55);
        }
        return preflightOutcomeAccepted(family, enhanceLevel, winRate, firstSideWinRate);
    }

    private static boolean preflightOutcomeAccepted(
        String acceptanceProtocol,
        MatrixFamily family,
        int enhanceLevel,
        double winRate,
        double firstSideWinRate
    ) {
        return acceptanceProtocol.equals("V4")
            ? v4PreflightOutcomeAccepted(family, enhanceLevel, winRate, firstSideWinRate)
            : preflightOutcomeAccepted(family, enhanceLevel, winRate, firstSideWinRate);
    }

    private static boolean between(double value, double minimum, double maximum) {
        return value >= minimum && value <= maximum;
    }

    private static String markdown(
        List<CellResult> results,
        int iterations,
        int maxRounds,
        long rootSeed,
        String mode,
        String acceptanceProtocol,
        boolean details
    ) {
        final var markdown = new StringBuilder()
            .append("# Слой исходов боевой матрицы шага 12\n\n")
            .append("Корневое начальное значение: `")
            .append(rootSeed)
            .append("`; пар начальных значений на ячейку: `")
            .append(iterations)
            .append("`; предел раундов: `")
            .append(maxRounds)
            .append("`. Парный 95% ДИ считает каждое начальное значение одним независимым наблюдением ")
            .append("с оценкой `0`, `1/2` или `1` по двум порядкам команд. Интервал построен ")
            .append("20 000 повторными выборками целых пар; границы — 500-я и 19 500-я оценки.\n\n")
            .append(acceptanceProtocol.equals("V4")
                ? "Боевой кандидат: `V3`; протокол приёмки: `V4`.\n\n"
                : "")
            .append(preflightDescription(mode, acceptanceProtocol))
            .append("Доли побед не подменяют причинную проверку: именованные ворота ")
            .append("исполняются и принимаются отдельно.\n\n")
            .append("| Ячейка | Семейство | A | B | Уровень | Размер | Начальное значение | ")
            .append("A первой | A второй | A, итог (парный 95% ДИ) | Первая сторона | Исход | Точность | ")
            .append("Отдельные причинные ворота |\n")
            .append("|---|---|---|---|---:|---:|---:|---:|---:|---:|---:|---|---|---|\n");
        for (final var result : results) {
            final var cell = result.cell();
            final var report = result.report();
            final boolean outcomeAccepted = mode.equals("PREFLIGHT")
                ? preflightOutcomeAccepted(
                    acceptanceProtocol,
                    cell.family(),
                    cell.enhanceLevel(),
                    report.evaluatedWinRate(),
                    report.firstSideWinRate()
                )
                : result.decision().outcomeAccepted();
            markdown.append(String.format(
                Locale.ROOT,
                "| %s | %s | %s | %s | +%d | %d | %d | %.2f%% | %.2f%% | %.2f%% [%.2f%%, %.2f%%] | "
                    + "%.2f%% | %s | %s | %s |%n",
                cell.code(),
                cell.family(),
                escape(cell.fixture().evaluatedLabel()),
                escape(cell.fixture().opponentsLabel()),
                cell.enhanceLevel(),
                cell.partySize(),
                cell.seed(),
                report.evaluatedFirst().winRate() * 100,
                (1 - report.opponentsFirst().winRate()) * 100,
                report.evaluatedWinRate() * 100,
                report.evaluatedWinRate95().lower() * 100,
                report.evaluatedWinRate95().upper() * 100,
                report.firstSideWinRate() * 100,
                mark(outcomeAccepted),
                mark(result.decision().precisionAccepted()),
                escape(result.decision().causalGateDescription())
            ));
        }
        appendStartingPositions(markdown, results);
        if (details) {
            appendDetailedReports(markdown, results);
        }
        return markdown.toString();
    }

    private static String preflightDescription(String mode, String acceptanceProtocol) {
        if (!mode.equals("PREFLIGHT")) {
            return "";
        }
        if (acceptanceProtocol.equals("V4")) {
            return "Это одноразовый предварительный прогон V4. Для зеркал и доля побед A, и доля побед "
                + "первой стороны должны попасть в 45–55%; для направленных боёв на +0, +10 и +20 "
                + "точечная доля побед должна попасть в 55–65%, а на +3 и +6 — быть выше 50%. "
                + "Доверительный интервал и его ширина показаны справочно и станут обязательными только "
                + "в итоговом прогоне.\n\n";
        }
        return "Это одноразовый предварительный прогон: решение использует точечную оценку исхода, а весь "
            + "доверительный интервал и его ширина показаны справочно и станут обязательными только "
            + "в итоговом прогоне.\n\n";
    }

    private static void appendStartingPositions(StringBuilder markdown, List<CellResult> results) {
        markdown.append("\n## Фактические начальные позиции после автоматического сближения\n\n")
            .append("| Ячейка | Порядок сторон | Позиции |\n")
            .append("|---|---|---|\n");
        for (final var result : results) {
            appendStartingPositions(
                markdown,
                result.cell().code(),
                "A → B",
                result.report().evaluatedFirst()
            );
            appendStartingPositions(
                markdown,
                result.cell().code(),
                "B → A",
                result.report().opponentsFirst()
            );
        }
    }

    private static void appendStartingPositions(
        StringBuilder markdown,
        String cellCode,
        String sideOrder,
        CombatSimulationReport report
    ) {
        final var positions = report.startingPositions().stream()
            .map(position -> "%s: %s→%s@%d, д=%d/%d".formatted(
                position.participant().displayName(),
                position.requestedLine(),
                position.actualLine(),
                position.actualLineIndex(),
                position.distanceToNearestEnemy(),
                position.range()
            ))
            .collect(Collectors.joining("; "));
        markdown.append("| ")
            .append(cellCode)
            .append(" | ")
            .append(sideOrder)
            .append(" | ")
            .append(escape(positions))
            .append(" |\n");
    }

    private static void appendDetailedReports(StringBuilder markdown, List<CellResult> results) {
        markdown.append("\n## Подробная телеметрия\n");
        for (final var result : results) {
            markdown.append("\n### ")
                .append(result.cell().code())
                .append(" — A идёт первой\n\n")
                .append(result.report().evaluatedFirst().markdown())
                .append("\n### ")
                .append(result.cell().code())
                .append(" — B идёт первой\n\n")
                .append(result.report().opponentsFirst().markdown());
        }
    }

    private static String mark(boolean accepted) {
        return accepted ? "принято" : "не принято";
    }

    private static String escape(String value) {
        return value.replace("|", "\\|");
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

    private static Set<String> presentFilterProperties() {
        return FILTER_PROPERTIES.stream()
            .filter(name -> System.getProperty(PREFIX + name) != null)
            .collect(Collectors.toUnmodifiableSet());
    }

    private static Set<Integer> intSetProperty(String name, List<Integer> defaults) {
        final var defaultValue = defaults.stream().map(String::valueOf).collect(Collectors.joining(","));
        return Arrays.stream(property(name, defaultValue).split(","))
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .map(Integer::parseInt)
            .collect(Collectors.toUnmodifiableSet());
    }

    enum MatrixFamily {
        ALL,
        MIRROR,
        FAVORABLE,
    }

    record MatrixRunConfiguration(
        String revision,
        String mode,
        int iterations,
        int maxRounds,
        long rootSeed,
        boolean enforce,
        Set<String> presentFilters,
        String acceptanceProtocol
    ) {
        MatrixRunConfiguration(
            String revision,
            String mode,
            int iterations,
            int maxRounds,
            long rootSeed,
            boolean enforce,
            Set<String> presentFilters
        ) {
            this(revision, mode, iterations, maxRounds, rootSeed, enforce, presentFilters, "V3");
        }

        MatrixRunConfiguration {
            revision = Objects.requireNonNull(revision);
            mode = Objects.requireNonNull(mode);
            presentFilters = Set.copyOf(presentFilters);
            acceptanceProtocol = Objects.requireNonNull(acceptanceProtocol);
        }

        boolean finalRun() {
            return Set.of("V2", "V3").contains(revision) && mode.equals("FINAL");
        }

        boolean preflightRun() {
            return revision.equals("V3") && mode.equals("PREFLIGHT");
        }

        boolean acceptanceRun() {
            return finalRun() || preflightRun();
        }

        boolean v3AcceptanceRun() {
            return revision.equals("V3") && acceptanceRun() && acceptanceProtocol.equals("V3");
        }

        boolean v4AcceptanceRun() {
            return revision.equals("V3") && acceptanceRun() && acceptanceProtocol.equals("V4");
        }
    }

    record MatrixCell(
        String code,
        MatrixFamily family,
        MatrixFixture fixture,
        int partySize,
        int enhanceLevel,
        long seed,
        CausalGate causalGate
    ) {
        MatrixCell {
            Objects.requireNonNull(fixture);
            Objects.requireNonNull(causalGate);
            if (family == MatrixFamily.MIRROR && !(fixture instanceof MirrorFixture)) {
                throw new IllegalArgumentException("Mirror cell requires a RoleBuild mirror fixture");
            }
            if (family == MatrixFamily.FAVORABLE && fixture instanceof MirrorFixture) {
                throw new IllegalArgumentException("Favorable cell requires a directed fixture");
            }
        }

        PairedCombatRequest request(int iterations, int maxRounds) {
            return new PairedCombatRequest(
                family.name(),
                code,
                fixture.evaluatedLabel(),
                fixture.opponentsLabel(),
                partySize,
                iterations,
                maxRounds,
                seed,
                () -> fixture.teams(partySize, enhanceLevel)
            );
        }
    }

    interface MatrixFixture {
        String evaluatedLabel();

        String opponentsLabel();

        CombatSimulationTeams teams(int partySize, int enhanceLevel);
    }

    record MirrorFixture(RoleBuild build, boolean currentRevision) implements MatrixFixture {
        @Override
        public String evaluatedLabel() {
            return build.displayName();
        }

        @Override
        public String opponentsLabel() {
            return build.displayName();
        }

        @Override
        public CombatSimulationTeams teams(int partySize, int enhanceLevel) {
            return currentRevision
                ? Step12SimulationFixtures.teams(build, build, partySize, enhanceLevel)
                : Step12SimulationFixtures.historicalV2Teams(build, build, partySize, enhanceLevel);
        }
    }

    record LegacyFavorableFixture(RoleBuild evaluated, RoleBuild opponents) implements MatrixFixture {
        @Override
        public String evaluatedLabel() {
            return evaluated.displayName();
        }

        @Override
        public String opponentsLabel() {
            return opponents.displayName();
        }

        @Override
        public CombatSimulationTeams teams(int partySize, int enhanceLevel) {
            return Step12SimulationFixtures.historicalV2Teams(
                evaluated,
                opponents,
                partySize,
                enhanceLevel
            );
        }
    }

    record V2FavorableFixture(V2Matchup matchup) implements MatrixFixture {
        @Override
        public String evaluatedLabel() {
            return teamLabel(matchup.evaluatedTeam());
        }

        @Override
        public String opponentsLabel() {
            return teamLabel(matchup.opponents());
        }

        @Override
        public CombatSimulationTeams teams(int partySize, int enhanceLevel) {
            if (partySize != 3) {
                throw new IllegalArgumentException("V2 favorable matchup requires party size 3");
            }
            return Step12SimulationFixtures.v2Teams(matchup, enhanceLevel);
        }

        private static String teamLabel(List<Step12SimulationFixtures.V2Placement> team) {
            return team.stream()
                .map(placement -> placement.build().displayName())
                .collect(Collectors.joining(" + "));
        }
    }

    record V3FavorableFixture(V3Matchup matchup) implements MatrixFixture {
        @Override
        public String evaluatedLabel() {
            return teamLabel(matchup.evaluatedTeam());
        }

        @Override
        public String opponentsLabel() {
            return teamLabel(matchup.opponents());
        }

        @Override
        public CombatSimulationTeams teams(int partySize, int enhanceLevel) {
            if (partySize != 3) {
                throw new IllegalArgumentException("V3 favorable matchup requires party size 3");
            }
            return Step12SimulationFixtures.v3Teams(matchup, enhanceLevel);
        }

        private static String teamLabel(List<Step12SimulationFixtures.V3Placement> team) {
            return team.stream()
                .map(placement -> placement.build().displayName())
                .collect(Collectors.joining(" + "));
        }
    }

    private record LegacyFavorableMatchup(String code, RoleBuild evaluated, RoleBuild opponents) {
    }

    enum CausalGate {
        NOT_REQUIRED(false, "не требуются для зеркальной ячейки"),
        NOT_REGISTERED(false, "не были заданы для первой редакции"),
        BRUISER(true, "отдельно: защита Стража, давление Громилы и порог «Берсерка»"),
        SKIRMISHER(true, "отдельно: отход Застрельщика и потеря атаки преследователем"),
        ASSASSIN(true, "отдельно: дальнее окно «Проникновения»"),
        RANGER(true, "отдельно: доля дальних попыток и снижение атаки при сближении"),
        BREAKER(true, "отдельно: разряд четвёртой попытки и сброс заряда"),
        TACTICIAN(true, "отдельно: снятие шкалы и задержка действий");

        private final boolean separateRequired;
        private final String description;

        CausalGate(boolean separateRequired, String description) {
            this.separateRequired = separateRequired;
            this.description = description;
        }

        boolean separateRequired() {
            return separateRequired;
        }

        String description() {
            return description;
        }

        static CausalGate forMatchup(V2Matchup matchup) {
            return switch (matchup) {
                case BRUISER_V2 -> BRUISER;
                case SKIRMISHER_V2 -> SKIRMISHER;
                case ASSASSIN_V2 -> ASSASSIN;
                case RANGER_V2 -> RANGER;
                case BREAKER_V2 -> BREAKER;
                case TACTICIAN_V2 -> TACTICIAN;
            };
        }

        static CausalGate forMatchup(V3Matchup matchup) {
            return switch (matchup) {
                case BRUISER_V3 -> BRUISER;
                case SKIRMISHER_V3 -> SKIRMISHER;
                case ASSASSIN_V3 -> ASSASSIN;
                case RANGER_V3 -> RANGER;
                case BREAKER_V3 -> BREAKER;
                case TACTICIAN_V3 -> TACTICIAN;
            };
        }
    }

    record Decision(
        boolean outcomeAccepted,
        boolean precisionAccepted,
        String causalGateDescription,
        String description
    ) {
        boolean outcomeLayerAccepted() {
            return outcomeAccepted && precisionAccepted;
        }
    }

    record CellResult(MatrixCell cell, PairedCombatReport report, Decision decision) {
    }
}
