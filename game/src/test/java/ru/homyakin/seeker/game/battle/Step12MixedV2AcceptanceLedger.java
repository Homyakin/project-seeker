package ru.homyakin.seeker.game.battle;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Immutable, one-use acceptance ledger for the second mixed-build protocol of step 12. */
public final class Step12MixedV2AcceptanceLedger {
    public static final long FINAL_ROOT_SEED = 2_026_092_701L;
    public static final int FINAL_ITERATIONS = 10_000;
    public static final int FINAL_MAX_ROUNDS = 10_000;
    public static final int FINAL_WORKERS = 4;
    public static final int FINAL_CELL_COUNT = 70;
    public static final int FINAL_HYPOTHESIS_COUNT = 105;

    static final String MANIFEST_RELATIVE_PATH =
        "documentation/raid-redesign-step-12-mixed-v2-inputs.sha256";
    static final String FINAL_MARKER_RELATIVE_PATH =
        "documentation/raid-redesign-step-12-mixed-v2-final-seed-2026092701.attempt.properties";
    static final String FINAL_EXECUTION_MARKER_RELATIVE_PATH =
        "documentation/raid-redesign-step-12-mixed-v2-final-seed-2026092701.battle-attempt.properties";
    static final String FINAL_REPORT_RELATIVE_PATH =
        "documentation/raid-redesign-step-12-mixed-v2-final.md";
    static final String FINAL_STATISTICS_RELATIVE_PATH =
        "documentation/raid-redesign-step-12-mixed-v2-final-statistics.tsv";
    static final String FINAL_LOG_RELATIVE_PATH =
        "documentation/raid-redesign-step-12-mixed-v2-final-2026092701.log";
    static final String FINAL_RECEIPT_RELATIVE_PATH =
        "documentation/raid-redesign-step-12-mixed-v2-final-2026092701.receipt.properties";

    static final List<String> EXACT_TEST_CLASSES = List.of(
        "BattleEventJsonTest",
        "BattleLocalizationTest",
        "BattleSkillInitSnapshotVersionTest",
        "CombatSimulatorTest",
        "CombatTelemetryTest",
        "EquipmentCatalogLoaderTest",
        "EquipmentCatalogValidatorTest",
        "InitGameDataTest",
        "NewScalingSkillsBattleIntegrationTest",
        "PenetrationTargetLockBattleTest",
        "ScalingCooldownsTest",
        "ScalingSkillBookTest",
        "ScalingSkillFormulaVersionBattleTest",
        "ScalingV2ExistingItemMigrationTest",
        "ScalingV2ItemCatalogTest",
        "ScalingV2ModifierCatalogTest",
        "ScalingV3CatalogRevisionTest",
        "Step12CatalogAcceptanceTest",
        "Step12CausalObservationRunnerTest",
        "Step12CausalStatisticsTest",
        "Step12CombatMatrixTest",
        "Step12InputManifestTest",
        "Step12MixedBuildFixturesTest",
        "Step12MixedBuildMatrixTest",
        "Step12MixedBuildObservationRunnerTest",
        "Step12MixedV2SimulatorTest",
        "Step12MixedBuildStatisticsTest",
        "Step12MixedV2AcceptanceLedgerTest",
        "Step12MixedV2InputManifestTest",
        "Step12MixedV2ObservationRunnerTest",
        "Step12OneShotBattleRandomTest",
        "Step12ScalingV3FixturesTest",
        "Step12V2SimulationFixturesTest",
        "Step12V3FormulaWiringTest",
        "Step12V3ObservationCorrectionTest",
        "VersionedAttemptThreatTest",
        "VersionedBattleBoundaryTest",
        "VersionedExistingSkillsBattleTest",
        "VersionedSkillIntegrationTest",
        "VersionedTeamSkillStateTest",
        "VersionedTurnExecutionTest",
        "WorldRaidPersonageSkillCompatibilityTest"
    );

    private static final String SCHEMA = "step12-mixed-v2-acceptance-v1";
    private static final String FINAL_KIND = "FINAL";
    private static final Set<String> VERIFICATION_RECEIPT_KEYS = Set.of(
        "schema", "status", "kind", "inputFingerprint", "mixedV1InputFingerprint",
        "mixedV1MainTreeSha256", "mixedV1EvidenceSetSha256",
        "command", "commandSha256", "log", "exitCode", "logSha256"
    );
    private static final Set<String> FINAL_RECEIPT_KEYS = Set.of(
        "schema", "status", "kind", "rootSeed", "iterations", "maxRounds", "workers",
        "cellCount", "hypothesisCount", "cellSetSha256", "hypothesisSetSha256",
        "inputFingerprint", "mixedV1InputFingerprint", "mixedV1MainTreeSha256", "mixedV1EvidenceSetSha256",
        "exactReceipt", "exactReceiptSha256",
        "fullBuildReceipt", "fullBuildReceiptSha256", "command", "commandSha256", "log",
        "report", "statistics", "exitCode", "logSha256", "reportSha256", "statisticsSha256"
    );

    private final Path repositoryRoot;
    private final InputVerifier inputVerifier;

    Step12MixedV2AcceptanceLedger(
        Path repositoryRoot,
        Step12MixedV2InputManifest.MixedV1Anchor mixedV1Anchor
    ) {
        this(
            repositoryRoot,
            root -> Step12MixedV2InputManifest.verifyAndFingerprint(
                root.resolve(MANIFEST_RELATIVE_PATH),
                root,
                mixedV1Anchor
            )
        );
    }

    Step12MixedV2AcceptanceLedger(Path repositoryRoot, InputVerifier inputVerifier) {
        this.repositoryRoot = canonicalDirectory(repositoryRoot);
        this.inputVerifier = Objects.requireNonNull(inputVerifier, "inputVerifier");
        if (!Files.isRegularFile(this.repositoryRoot.resolve("game/pom.xml"))
            || !Files.isDirectory(this.repositoryRoot.resolve("documentation"))) {
            throw new IllegalArgumentException(
                "Step 12 repository root must contain game/pom.xml and documentation: "
                    + this.repositoryRoot
            );
        }
    }

    public static Step12MixedV2AcceptanceLedger fromTestClasses(Class<?> anchor) {
        Objects.requireNonNull(anchor, "anchor");
        final Path codeLocation;
        try {
            codeLocation = Path.of(anchor.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException | NullPointerException exception) {
            throw new IllegalStateException("Cannot resolve test-classes location", exception);
        }
        return new Step12MixedV2AcceptanceLedger(
            findRepositoryRoot(codeLocation),
            new Step12MixedV2InputManifest.MixedV1Anchor(
                Step12MixedV2InputManifest.MIXED_V1_MANIFEST_FINGERPRINT,
                Step12MixedV2InputManifest.MIXED_V1_MAIN_TREE_SHA256,
                Step12MixedV2InputManifest.MIXED_V1_EVIDENCE_SET_SHA256
            )
        );
    }

    static Path findRepositoryRoot(Path codeLocation) {
        Objects.requireNonNull(codeLocation, "codeLocation");
        Path current;
        try {
            final var canonicalLocation = codeLocation.toRealPath();
            current = Files.isDirectory(canonicalLocation)
                ? canonicalLocation
                : canonicalLocation.getParent();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot resolve test-classes location: " + codeLocation, exception);
        }
        while (current != null) {
            if (Files.isRegularFile(current.resolve("game/pom.xml"))
                && Files.isDirectory(current.resolve("documentation"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot find repository root above " + codeLocation);
    }

    /** Starts the official run and burns its seed before any battle can execute. */
    public FinalAttempt beginFinal(
        long rootSeed,
        int iterations,
        int maxRounds,
        int workers,
        Collection<String> cellDescriptors,
        Collection<String> hypothesisDescriptors,
        String explicitlyConfiguredOutput,
        String explicitlyConfiguredStatistics
    ) {
        validateFinalParameters(rootSeed, iterations, maxRounds, workers);
        validateExplicitOutputs(explicitlyConfiguredOutput, explicitlyConfiguredStatistics);
        final var canonicalCells = canonicalDescriptors(cellDescriptors, "cell");
        final var expectedCells = canonicalDescriptors(expectedFinalCellDescriptors(), "expected cell");
        if (!canonicalCells.equals(expectedCells) || canonicalCells.size() != FINAL_CELL_COUNT) {
            throw new IllegalArgumentException("Official mixed run requires the exact 70-cell set");
        }
        final var canonicalHypotheses = canonicalDescriptors(hypothesisDescriptors, "hypothesis");
        final var expectedHypotheses = canonicalDescriptors(
            expectedFinalHypothesisDescriptors(),
            "expected hypothesis"
        );
        if (!canonicalHypotheses.equals(expectedHypotheses)
            || canonicalHypotheses.size() != FINAL_HYPOTHESIS_COUNT) {
            throw new IllegalArgumentException("Official mixed run requires the exact 105-hypothesis family");
        }

        final var inputFingerprint = verifyInputs();
        final var exact = verifyVerificationReceipt(VerificationKind.EXACT_TESTS, inputFingerprint);
        final var full = verifyVerificationReceipt(VerificationKind.FULL_BUILD, inputFingerprint);
        final var marker = resolve(FINAL_MARKER_RELATIVE_PATH);
        final var executionMarker = resolve(FINAL_EXECUTION_MARKER_RELATIVE_PATH);
        final var report = resolve(FINAL_REPORT_RELATIVE_PATH);
        final var statistics = resolve(FINAL_STATISTICS_RELATIVE_PATH);
        final var log = resolve(FINAL_LOG_RELATIVE_PATH);
        final var receipt = resolve(FINAL_RECEIPT_RELATIVE_PATH);
        rejectPreexistingArtifact(marker, "official seed marker");
        rejectPreexistingArtifact(executionMarker, "official battle-attempt marker");
        rejectPreexistingArtifact(report, "final report");
        rejectPreexistingArtifact(statistics, "final exact statistics");
        rejectPreexistingArtifact(log, "final log");
        rejectPreexistingArtifact(receipt, "final receipt");

        final var attempt = new FinalAttempt(
            repositoryRoot,
            marker,
            report,
            statistics,
            log,
            receipt,
            inputFingerprint,
            descriptorFingerprint(canonicalCells),
            descriptorFingerprint(canonicalHypotheses),
            exact.receiptRelativePath(),
            exact.receiptSha256(),
            full.receiptRelativePath(),
            full.receiptSha256()
        );
        writeNew(marker, encode(finalFields(attempt, "STARTED")), "official seed marker");
        writeNew(log, "", "final log");
        return attempt;
    }

    /**
     * Atomically claims the only battle execution after the outer launcher has burned the seed.
     * This is called by the simulator before its first observation, so a direct or repeated FINAL
     * invocation cannot execute battles outside the protected acceptance chain.
     */
    public FinalAttempt claimFinalExecution(
        long rootSeed,
        int iterations,
        int maxRounds,
        int workers,
        Collection<String> cellDescriptors,
        Collection<String> hypothesisDescriptors,
        String explicitlyConfiguredOutput,
        String explicitlyConfiguredStatistics
    ) {
        validateFinalParameters(rootSeed, iterations, maxRounds, workers);
        validateExplicitOutputs(explicitlyConfiguredOutput, explicitlyConfiguredStatistics);
        final var canonicalCells = canonicalDescriptors(cellDescriptors, "cell");
        final var expectedCells = canonicalDescriptors(expectedFinalCellDescriptors(), "expected cell");
        if (!canonicalCells.equals(expectedCells) || canonicalCells.size() != FINAL_CELL_COUNT) {
            throw new IllegalArgumentException("Official mixed run requires the exact 70-cell set");
        }
        final var canonicalHypotheses = canonicalDescriptors(hypothesisDescriptors, "hypothesis");
        final var expectedHypotheses = canonicalDescriptors(
            expectedFinalHypothesisDescriptors(),
            "expected hypothesis"
        );
        if (!canonicalHypotheses.equals(expectedHypotheses)
            || canonicalHypotheses.size() != FINAL_HYPOTHESIS_COUNT) {
            throw new IllegalArgumentException("Official mixed run requires the exact 105-hypothesis family");
        }

        final var inputFingerprint = verifyInputs();
        final var exact = verifyVerificationReceipt(VerificationKind.EXACT_TESTS, inputFingerprint);
        final var full = verifyVerificationReceipt(VerificationKind.FULL_BUILD, inputFingerprint);
        final var attempt = new FinalAttempt(
            repositoryRoot,
            resolve(FINAL_MARKER_RELATIVE_PATH),
            resolve(FINAL_REPORT_RELATIVE_PATH),
            resolve(FINAL_STATISTICS_RELATIVE_PATH),
            resolve(FINAL_LOG_RELATIVE_PATH),
            resolve(FINAL_RECEIPT_RELATIVE_PATH),
            inputFingerprint,
            descriptorFingerprint(canonicalCells),
            descriptorFingerprint(canonicalHypotheses),
            exact.receiptRelativePath(),
            exact.receiptSha256(),
            full.receiptRelativePath(),
            full.receiptSha256()
        );
        verifyFinalMarker(attempt);
        if (!Files.isRegularFile(attempt.log())) {
            throw new IllegalStateException("Official final log is missing: " + attempt.log());
        }
        rejectPreexistingArtifact(attempt.report(), "final report");
        rejectPreexistingArtifact(attempt.statistics(), "final exact statistics");
        rejectPreexistingArtifact(attempt.receipt(), "final receipt");
        writeNew(
            resolve(FINAL_EXECUTION_MARKER_RELATIVE_PATH),
            encode(finalFields(attempt, "BATTLE_STARTED")),
            "official battle-attempt marker"
        );
        return attempt;
    }

    /** Writes exact sufficient statistics with CREATE_NEW before the rounded report. */
    public void writeFinalStatistics(FinalAttempt attempt, String statistics) {
        Objects.requireNonNull(statistics, "statistics");
        validateAttempt(attempt);
        verifyFinalMarker(attempt);
        verifyFinalExecutionMarker(attempt);
        writeNew(attempt.statistics(), statistics, "final exact statistics");
    }

    /** Writes the fixed report with CREATE_NEW; a rejected run deliberately keeps it. */
    public void writeFinalReport(FinalAttempt attempt, String markdown) {
        Objects.requireNonNull(markdown, "markdown");
        validateAttempt(attempt);
        verifyFinalMarker(attempt);
        verifyFinalExecutionMarker(attempt);
        if (!Files.isRegularFile(attempt.statistics())) {
            throw new IllegalStateException("Final exact statistics are missing: " + attempt.statistics());
        }
        writeNew(attempt.report(), markdown, "final report");
    }

    /** Creates SUCCESS only after every pre-registered gate has been accepted. */
    public FinalReceipt completeFinal(FinalAttempt attempt, boolean allGatesAccepted) {
        validateAttempt(attempt);
        verifyFinalMarker(attempt);
        verifyFinalExecutionMarker(attempt);
        if (!Files.isRegularFile(attempt.report())) {
            throw new IllegalStateException("Final report is missing: " + attempt.report());
        }
        if (!Files.isRegularFile(attempt.statistics())) {
            throw new IllegalStateException("Final exact statistics are missing: " + attempt.statistics());
        }
        if (!allGatesAccepted) {
            throw new IllegalStateException("Mixed-build gates were not all accepted; no success receipt created");
        }
        final var currentFingerprint = verifyInputs();
        if (!currentFingerprint.equals(attempt.inputFingerprint())) {
            throw new IllegalStateException("Mixed acceptance inputs changed during the official run");
        }
        requireReceiptHash(attempt.exactReceiptRelativePath(), attempt.exactReceiptSha256());
        requireReceiptHash(attempt.fullBuildReceiptRelativePath(), attempt.fullBuildReceiptSha256());
        final var fields = finalFields(attempt, "SUCCESS");
        fields.put("exitCode", "0");
        fields.put("logSha256", fileSha256(attempt.log()));
        fields.put("reportSha256", fileSha256(attempt.report()));
        fields.put("statisticsSha256", fileSha256(attempt.statistics()));
        writeNew(attempt.receipt(), encode(fields), "final success receipt");
        return new FinalReceipt(
            attempt.inputFingerprint(),
            attempt.cellSetSha256(),
            attempt.hypothesisSetSha256(),
            attempt.report(),
            fields.get("reportSha256"),
            attempt.statistics(),
            fields.get("statisticsSha256"),
            attempt.log(),
            fields.get("logSha256")
        );
    }

    VerificationReceipt runVerification(
        VerificationKind kind,
        VerificationCommandExecutor executor
    ) {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(executor, "executor");
        final var inputFingerprint = verifyInputs();
        if (kind == VerificationKind.FULL_BUILD) {
            verifyVerificationReceipt(VerificationKind.EXACT_TESTS, inputFingerprint);
        }
        final var marker = resolve(kind.markerRelativePath(inputFingerprint));
        final var log = resolve(kind.logRelativePath(inputFingerprint));
        final var receipt = resolve(kind.receiptRelativePath(inputFingerprint));
        rejectPreexistingArtifact(marker, "verification marker");
        rejectPreexistingArtifact(log, "verification log");
        rejectPreexistingArtifact(receipt, "verification receipt");
        final var fields = verificationFields(kind, inputFingerprint, "STARTED");
        writeNew(marker, encode(fields), "verification marker");
        writeNew(log, "", "verification log");

        final int exitCode;
        try {
            exitCode = executor.execute(kind.commandTokens(), repositoryRoot.resolve("game"), log);
        } catch (Exception exception) {
            throw new IllegalStateException("Mixed verification command failed to execute: " + kind, exception);
        }
        if (exitCode != 0) {
            throw new IllegalStateException(
                "Mixed verification command failed with exit code " + exitCode + ": " + kind
            );
        }
        if (!verifyInputs().equals(inputFingerprint)) {
            throw new IllegalStateException("Mixed acceptance inputs changed during verification");
        }
        final var receiptFields = verificationFields(kind, inputFingerprint, "SUCCESS");
        receiptFields.put("exitCode", "0");
        receiptFields.put("logSha256", fileSha256(log));
        writeNew(receipt, encode(receiptFields), "verification success receipt");
        return new VerificationReceipt(
            kind,
            inputFingerprint,
            kind.command(),
            kind.receiptRelativePath(inputFingerprint),
            fileSha256(receipt),
            log,
            receiptFields.get("logSha256")
        );
    }

    /** Recomputes the complete evidence chain without writing any artifact. */
    public FinalEvidence verifyFinalEvidence() {
        final var inputFingerprint = verifyInputs();
        final var exact = verifyVerificationReceipt(VerificationKind.EXACT_TESTS, inputFingerprint);
        final var full = verifyVerificationReceipt(VerificationKind.FULL_BUILD, inputFingerprint);
        final var receiptPath = resolve(FINAL_RECEIPT_RELATIVE_PATH);
        final var fields = readFields(receiptPath, FINAL_RECEIPT_KEYS);
        requireField(fields, "schema", SCHEMA, receiptPath);
        requireField(fields, "status", "SUCCESS", receiptPath);
        requireField(fields, "kind", FINAL_KIND, receiptPath);
        requireField(fields, "rootSeed", Long.toString(FINAL_ROOT_SEED), receiptPath);
        requireField(fields, "iterations", Integer.toString(FINAL_ITERATIONS), receiptPath);
        requireField(fields, "maxRounds", Integer.toString(FINAL_MAX_ROUNDS), receiptPath);
        requireField(fields, "workers", Integer.toString(FINAL_WORKERS), receiptPath);
        requireField(fields, "cellCount", Integer.toString(FINAL_CELL_COUNT), receiptPath);
        requireField(fields, "hypothesisCount", Integer.toString(FINAL_HYPOTHESIS_COUNT), receiptPath);
        requireField(fields, "cellSetSha256", expectedCellSetSha256(), receiptPath);
        requireField(fields, "hypothesisSetSha256", expectedHypothesisSetSha256(), receiptPath);
        requireField(fields, "inputFingerprint", inputFingerprint, receiptPath);
        requireField(
            fields,
            "mixedV1InputFingerprint",
            Step12MixedV2InputManifest.MIXED_V1_MANIFEST_FINGERPRINT,
            receiptPath
        );
        requireField(
            fields,
            "mixedV1MainTreeSha256",
            Step12MixedV2InputManifest.MIXED_V1_MAIN_TREE_SHA256,
            receiptPath
        );
        requireField(
            fields,
            "mixedV1EvidenceSetSha256",
            Step12MixedV2InputManifest.MIXED_V1_EVIDENCE_SET_SHA256,
            receiptPath
        );
        requireField(fields, "exactReceipt", exact.receiptRelativePath(), receiptPath);
        requireField(fields, "exactReceiptSha256", exact.receiptSha256(), receiptPath);
        requireField(fields, "fullBuildReceipt", full.receiptRelativePath(), receiptPath);
        requireField(fields, "fullBuildReceiptSha256", full.receiptSha256(), receiptPath);
        requireField(fields, "command", finalCommand(), receiptPath);
        requireField(fields, "commandSha256", sha256(finalCommand()), receiptPath);
        requireField(fields, "log", FINAL_LOG_RELATIVE_PATH, receiptPath);
        requireField(fields, "report", FINAL_REPORT_RELATIVE_PATH, receiptPath);
        requireField(fields, "statistics", FINAL_STATISTICS_RELATIVE_PATH, receiptPath);
        requireField(fields, "exitCode", "0", receiptPath);
        requireField(fields, "logSha256", fileSha256(resolve(FINAL_LOG_RELATIVE_PATH)), receiptPath);
        requireField(fields, "reportSha256", fileSha256(resolve(FINAL_REPORT_RELATIVE_PATH)), receiptPath);
        requireField(
            fields,
            "statisticsSha256",
            fileSha256(resolve(FINAL_STATISTICS_RELATIVE_PATH)),
            receiptPath
        );

        final var markerFields = new LinkedHashMap<>(fields);
        markerFields.remove("exitCode");
        markerFields.remove("logSha256");
        markerFields.remove("reportSha256");
        markerFields.remove("statisticsSha256");
        markerFields.put("status", "STARTED");
        final var markerPath = resolve(FINAL_MARKER_RELATIVE_PATH);
        if (!readFields(markerPath, markerFields.keySet()).equals(markerFields)) {
            throw new IllegalStateException("Official seed marker is missing or inconsistent");
        }
        markerFields.put("status", "BATTLE_STARTED");
        final var executionMarkerPath = resolve(FINAL_EXECUTION_MARKER_RELATIVE_PATH);
        if (!readFields(executionMarkerPath, markerFields.keySet()).equals(markerFields)) {
            throw new IllegalStateException("Official battle-attempt marker is missing or inconsistent");
        }
        return new FinalEvidence(
            inputFingerprint,
            exact,
            full,
            fields.get("reportSha256"),
            fields.get("statisticsSha256")
        );
    }

    public static List<String> expectedFinalCellDescriptors() {
        final var result = new ArrayList<String>();
        for (final var cell : Step12MixedBuildMatrix.cells(FINAL_ROOT_SEED)) {
            final var comparison = cell.comparison();
            final var worlds = new ArrayList<String>();
            worlds.add("MIXED_ON");
            if (comparison.activityMetric().isPresent()) {
                worlds.add("MIXED_OFF");
            }
            if (comparison.referenceBuild().isPresent()) {
                worlds.add("REFERENCE");
            }
            result.add(String.join("|",
                "CELL",
                cell.code(),
                "seed=" + cell.seed(),
                "mixed=" + comparison.mixedBuild().name(),
                "reference=" + comparison.referenceBuild().map(Enum::name).orElse("NONE"),
                "benchmark=" + comparison.benchmark().name(),
                "activitySkill=" + comparison.activitySkill().map(Enum::name).orElse("NONE"),
                "activityMetric=" + comparison.activityMetric().map(Enum::name).orElse("NONE"),
                "priceMetric=" + comparison.priceMetric().map(Enum::name).orElse("NONE"),
                "referenceMetric=" + comparison.referenceMetric().map(Enum::name).orElse("NONE"),
                "rule=" + comparison.rule().name(),
                "level=" + cell.level(),
                "worlds=" + String.join(",", worlds),
                "orders=FORWARD,REVERSED"
            ));
        }
        if (result.size() != FINAL_CELL_COUNT) {
            throw new IllegalStateException("Mixed matrix must define exactly 70 official cells");
        }
        return List.copyOf(result);
    }

    public static List<String> expectedFinalHypothesisDescriptors() {
        final var result = new ArrayList<String>();
        for (final var cell : Step12MixedBuildMatrix.cells(FINAL_ROOT_SEED)) {
            final var comparison = cell.comparison();
            comparison.activityMetric().ifPresent(metric -> result.add(String.join("|",
                cell.code() + "_ACTIVITY",
                "difference=MIXED_ON-MIXED_OFF",
                "metric=" + metric.name(),
                "direction=POSITIVE",
                "family=HOLM_105"
            )));
            comparison.priceMetric().ifPresent(metric -> result.add(String.join("|",
                cell.code() + "_PRICE",
                "difference=REFERENCE-MIXED_ON",
                "referenceMetric=" + comparison.referenceMetric().orElseThrow().name(),
                "mixedMetric=" + metric.name(),
                "direction=POSITIVE",
                "family=HOLM_105"
            )));
            if (comparison.rule() == Step12MixedBuildMatrix.Rule.CONTROL_DIAGNOSTIC) {
                result.add(String.join("|",
                    cell.code() + "_CONTROL_LOSS",
                    "difference=MIXED_ON-MIXED_OFF",
                    "metric=OPPONENT_MOVEMENT_ONLY_TURN_SHARE",
                    "direction=POSITIVE",
                    "family=HOLM_105"
                ));
            }
        }
        if (result.size() != FINAL_HYPOTHESIS_COUNT) {
            throw new IllegalStateException("Mixed matrix must define exactly 105 official hypotheses");
        }
        return List.copyOf(result);
    }

    static String expectedCellSetSha256() {
        return descriptorFingerprint(canonicalDescriptors(expectedFinalCellDescriptors(), "cell"));
    }

    static String expectedHypothesisSetSha256() {
        return descriptorFingerprint(canonicalDescriptors(expectedFinalHypothesisDescriptors(), "hypothesis"));
    }

    static List<String> finalCommandTokens() {
        return List.of(
            "mvn",
            "-Dtest=Step12MixedV2Simulator",
            "-Dstep12.mixed.v2.enabled=true",
            "-Dstep12.mixed.v2.mode=FINAL",
            "-Dstep12.mixed.v2.seed=" + FINAL_ROOT_SEED,
            "-Dstep12.mixed.v2.iterations=" + FINAL_ITERATIONS,
            "-Dstep12.mixed.v2.maxRounds=" + FINAL_MAX_ROUNDS,
            "-Dstep12.mixed.v2.workers=" + FINAL_WORKERS,
            "-Dstep12.mixed.v2.enforce=true",
            "-Dstep12.mixed.v2.scenario=",
            "-Dstep12.mixed.v2.output=../" + FINAL_REPORT_RELATIVE_PATH,
            "-Dstep12.mixed.v2.statistics=../" + FINAL_STATISTICS_RELATIVE_PATH,
            "-Dcheckstyle.skip=false",
            "-DskipTests=false",
            "-Dmaven.test.skip=false",
            "-Dsurefire.failIfNoSpecifiedTests=true",
            "clean",
            "test"
        );
    }

    static String finalCommand() {
        return String.join(" ", finalCommandTokens());
    }

    private VerificationReceipt verifyVerificationReceipt(
        VerificationKind kind,
        String inputFingerprint
    ) {
        final var receiptRelativePath = kind.receiptRelativePath(inputFingerprint);
        final var receipt = resolve(receiptRelativePath);
        final var fields = readFields(receipt, VERIFICATION_RECEIPT_KEYS);
        requireField(fields, "schema", SCHEMA, receipt);
        requireField(fields, "status", "SUCCESS", receipt);
        requireField(fields, "kind", kind.name(), receipt);
        requireField(fields, "inputFingerprint", inputFingerprint, receipt);
        requireField(
            fields,
            "mixedV1InputFingerprint",
            Step12MixedV2InputManifest.MIXED_V1_MANIFEST_FINGERPRINT,
            receipt
        );
        requireField(
            fields,
            "mixedV1MainTreeSha256",
            Step12MixedV2InputManifest.MIXED_V1_MAIN_TREE_SHA256,
            receipt
        );
        requireField(
            fields,
            "mixedV1EvidenceSetSha256",
            Step12MixedV2InputManifest.MIXED_V1_EVIDENCE_SET_SHA256,
            receipt
        );
        requireField(fields, "command", kind.command(), receipt);
        requireField(fields, "commandSha256", sha256(kind.command()), receipt);
        requireField(fields, "log", kind.logRelativePath(inputFingerprint), receipt);
        requireField(fields, "exitCode", "0", receipt);
        final var log = resolve(kind.logRelativePath(inputFingerprint));
        requireField(fields, "logSha256", fileSha256(log), receipt);

        final var markerFields = new LinkedHashMap<>(fields);
        markerFields.remove("exitCode");
        markerFields.remove("logSha256");
        markerFields.put("status", "STARTED");
        final var marker = resolve(kind.markerRelativePath(inputFingerprint));
        if (!readFields(marker, markerFields.keySet()).equals(markerFields)) {
            throw new IllegalStateException("Verification marker is missing or inconsistent: " + marker);
        }
        return new VerificationReceipt(
            kind,
            inputFingerprint,
            kind.command(),
            receiptRelativePath,
            fileSha256(receipt),
            log,
            fields.get("logSha256")
        );
    }

    private LinkedHashMap<String, String> verificationFields(
        VerificationKind kind,
        String inputFingerprint,
        String status
    ) {
        final var fields = new LinkedHashMap<String, String>();
        fields.put("schema", SCHEMA);
        fields.put("status", status);
        fields.put("kind", kind.name());
        fields.put("inputFingerprint", inputFingerprint);
        fields.put("mixedV1InputFingerprint", Step12MixedV2InputManifest.MIXED_V1_MANIFEST_FINGERPRINT);
        fields.put("mixedV1MainTreeSha256", Step12MixedV2InputManifest.MIXED_V1_MAIN_TREE_SHA256);
        fields.put("mixedV1EvidenceSetSha256", Step12MixedV2InputManifest.MIXED_V1_EVIDENCE_SET_SHA256);
        fields.put("command", kind.command());
        fields.put("commandSha256", sha256(kind.command()));
        fields.put("log", kind.logRelativePath(inputFingerprint));
        return fields;
    }

    private LinkedHashMap<String, String> finalFields(FinalAttempt attempt, String status) {
        final var fields = new LinkedHashMap<String, String>();
        fields.put("schema", SCHEMA);
        fields.put("status", status);
        fields.put("kind", FINAL_KIND);
        fields.put("rootSeed", Long.toString(FINAL_ROOT_SEED));
        fields.put("iterations", Integer.toString(FINAL_ITERATIONS));
        fields.put("maxRounds", Integer.toString(FINAL_MAX_ROUNDS));
        fields.put("workers", Integer.toString(FINAL_WORKERS));
        fields.put("cellCount", Integer.toString(FINAL_CELL_COUNT));
        fields.put("hypothesisCount", Integer.toString(FINAL_HYPOTHESIS_COUNT));
        fields.put("cellSetSha256", attempt.cellSetSha256());
        fields.put("hypothesisSetSha256", attempt.hypothesisSetSha256());
        fields.put("inputFingerprint", attempt.inputFingerprint());
        fields.put("mixedV1InputFingerprint", Step12MixedV2InputManifest.MIXED_V1_MANIFEST_FINGERPRINT);
        fields.put("mixedV1MainTreeSha256", Step12MixedV2InputManifest.MIXED_V1_MAIN_TREE_SHA256);
        fields.put("mixedV1EvidenceSetSha256", Step12MixedV2InputManifest.MIXED_V1_EVIDENCE_SET_SHA256);
        fields.put("exactReceipt", attempt.exactReceiptRelativePath());
        fields.put("exactReceiptSha256", attempt.exactReceiptSha256());
        fields.put("fullBuildReceipt", attempt.fullBuildReceiptRelativePath());
        fields.put("fullBuildReceiptSha256", attempt.fullBuildReceiptSha256());
        fields.put("command", finalCommand());
        fields.put("commandSha256", sha256(finalCommand()));
        fields.put("log", FINAL_LOG_RELATIVE_PATH);
        fields.put("report", FINAL_REPORT_RELATIVE_PATH);
        fields.put("statistics", FINAL_STATISTICS_RELATIVE_PATH);
        return fields;
    }

    private void validateAttempt(FinalAttempt attempt) {
        Objects.requireNonNull(attempt, "attempt");
        if (!attempt.repositoryRoot().equals(repositoryRoot)
            || !attempt.marker().equals(resolve(FINAL_MARKER_RELATIVE_PATH))
            || !attempt.report().equals(resolve(FINAL_REPORT_RELATIVE_PATH))
            || !attempt.statistics().equals(resolve(FINAL_STATISTICS_RELATIVE_PATH))
            || !attempt.log().equals(resolve(FINAL_LOG_RELATIVE_PATH))
            || !attempt.receipt().equals(resolve(FINAL_RECEIPT_RELATIVE_PATH))) {
            throw new IllegalArgumentException("Final attempt does not belong to this ledger");
        }
    }

    private void verifyFinalMarker(FinalAttempt attempt) {
        final var expected = finalFields(attempt, "STARTED");
        if (!readFields(attempt.marker(), expected.keySet()).equals(expected)) {
            throw new IllegalStateException("Official seed marker is corrupt: " + attempt.marker());
        }
    }

    private void verifyFinalExecutionMarker(FinalAttempt attempt) {
        final var expected = finalFields(attempt, "BATTLE_STARTED");
        final var marker = resolve(FINAL_EXECUTION_MARKER_RELATIVE_PATH);
        if (!readFields(marker, expected.keySet()).equals(expected)) {
            throw new IllegalStateException("Official battle-attempt marker is corrupt: " + marker);
        }
    }

    private String verifyInputs() {
        return inputVerifier.verify(repositoryRoot);
    }

    private void requireReceiptHash(String relativePath, String expectedSha256) {
        final var receipt = resolve(relativePath);
        if (!fileSha256(receipt).equals(expectedSha256)) {
            throw new IllegalStateException("Prerequisite receipt changed: " + receipt);
        }
    }

    private static void validateFinalParameters(long rootSeed, int iterations, int maxRounds, int workers) {
        final var violations = new ArrayList<String>();
        if (rootSeed != FINAL_ROOT_SEED) {
            violations.add("root seed must equal " + FINAL_ROOT_SEED);
        }
        if (iterations != FINAL_ITERATIONS) {
            violations.add("iterations must equal " + FINAL_ITERATIONS);
        }
        if (maxRounds != FINAL_MAX_ROUNDS) {
            violations.add("maxRounds must equal " + FINAL_MAX_ROUNDS);
        }
        if (workers != FINAL_WORKERS) {
            violations.add("workers must equal " + FINAL_WORKERS);
        }
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException("Invalid official mixed run: " + String.join("; ", violations));
        }
    }

    private void validateExplicitOutputs(
        String explicitlyConfiguredOutput,
        String explicitlyConfiguredStatistics
    ) {
        validateExplicitOutput(
            explicitlyConfiguredOutput,
            FINAL_REPORT_RELATIVE_PATH,
            "report"
        );
        validateExplicitOutput(
            explicitlyConfiguredStatistics,
            FINAL_STATISTICS_RELATIVE_PATH,
            "statistics"
        );
    }

    private void validateExplicitOutput(String value, String relativePath, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Acceptance " + name + " output must not be blank");
        }
        final var configured = Path.of(value);
        final var absolute = configured.isAbsolute()
            ? configured.toAbsolutePath().normalize()
            : repositoryRoot.resolve(configured).normalize();
        if (!absolute.equals(resolve(relativePath))) {
            throw new IllegalArgumentException("Acceptance " + name + " output is fixed at " + relativePath);
        }
    }

    private static List<String> canonicalDescriptors(Collection<String> descriptors, String name) {
        Objects.requireNonNull(descriptors, name + " descriptors");
        if (descriptors.isEmpty()) {
            throw new IllegalArgumentException(name + " descriptor set must not be empty");
        }
        final var sorted = new ArrayList<String>(descriptors.size());
        for (final var descriptor : descriptors) {
            if (descriptor == null || descriptor.isBlank()
                || descriptor.contains("\n") || descriptor.contains("\r")) {
                throw new IllegalArgumentException(name + " descriptors must be non-blank single lines");
            }
            sorted.add(descriptor);
        }
        sorted.sort(String::compareTo);
        if (Set.copyOf(sorted).size() != sorted.size()) {
            throw new IllegalArgumentException(name + " descriptors must be unique");
        }
        return List.copyOf(sorted);
    }

    private static String descriptorFingerprint(List<String> canonicalDescriptors) {
        return sha256(String.join("\n", canonicalDescriptors) + "\n");
    }

    private static Map<String, String> readFields(Path path, Set<String> expectedKeys) {
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException("Acceptance ledger file is missing: " + path);
        }
        final var fields = new LinkedHashMap<String, String>();
        final List<String> lines;
        try {
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read acceptance ledger file: " + path, exception);
        }
        for (final var line : lines) {
            final int separator = line.indexOf('=');
            if (separator <= 0) {
                throw new IllegalStateException("Invalid acceptance ledger line in " + path + ": " + line);
            }
            final var key = line.substring(0, separator);
            final var value = line.substring(separator + 1);
            if (fields.putIfAbsent(key, value) != null) {
                throw new IllegalStateException("Duplicate acceptance ledger key in " + path + ": " + key);
            }
        }
        if (!fields.keySet().equals(expectedKeys)) {
            throw new IllegalStateException("Unexpected acceptance ledger fields in " + path);
        }
        return Map.copyOf(fields);
    }

    private static String encode(Map<String, String> fields) {
        final var result = new StringBuilder();
        fields.forEach((key, value) -> {
            if (key.contains("=") || key.contains("\n") || value.contains("\n") || value.contains("\r")) {
                throw new IllegalArgumentException("Acceptance fields must be single-line key/value pairs");
            }
            result.append(key).append('=').append(value).append('\n');
        });
        return result.toString();
    }

    private static void writeNew(Path path, String content, String artifactName) {
        try {
            Files.writeString(
                path,
                content,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE
            );
        } catch (FileAlreadyExistsException exception) {
            throw new IllegalStateException(
                "One-use " + artifactName + " already exists; this action cannot be repeated: " + path,
                exception
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot create " + artifactName + ": " + path, exception);
        }
    }

    private static void rejectPreexistingArtifact(Path path, String artifactName) {
        if (Files.exists(path)) {
            throw new IllegalStateException("Preexisting " + artifactName + " blocks the action: " + path);
        }
    }

    private static void requireField(Map<String, String> fields, String key, String expected, Path receipt) {
        if (!expected.equals(fields.get(key))) {
            throw new IllegalStateException("Invalid " + key + " in acceptance receipt: " + receipt);
        }
    }

    private Path resolve(String relativePath) {
        final var resolved = repositoryRoot.resolve(relativePath).normalize();
        if (!resolved.startsWith(repositoryRoot)) {
            throw new IllegalStateException("Acceptance artifact escapes repository root: " + relativePath);
        }
        return resolved;
    }

    Path repositoryRoot() {
        return repositoryRoot;
    }

    private static Path canonicalDirectory(Path path) {
        try {
            final var canonical = path.toAbsolutePath().normalize().toRealPath();
            if (!Files.isDirectory(canonical)) {
                throw new IllegalArgumentException("Repository root is not a directory: " + canonical);
            }
            return canonical;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Cannot resolve repository root: " + path, exception);
        }
    }

    private static String fileSha256(Path path) {
        return Step12MixedV2InputManifest.fileSha256(path);
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public enum VerificationKind {
        EXACT_TESTS(
            "documentation/raid-redesign-step-12-mixed-v2-exact-tests",
            List.of(
                "mvn",
                "-Dtest=" + String.join(",", EXACT_TEST_CLASSES),
                "-Dcheckstyle.skip=false",
                "-DskipTests=false",
                "-Dmaven.test.skip=false",
                "-Dsurefire.failIfNoSpecifiedTests=true",
                "clean",
                "test"
            )
        ),
        FULL_BUILD(
            "documentation/raid-redesign-step-12-mixed-v2-full-build",
            List.of(
                "mvn",
                "-Dcheckstyle.skip=false",
                "-DskipTests=false",
                "-Dmaven.test.skip=false",
                "clean",
                "package"
            )
        );

        private final String artifactPrefix;
        private final List<String> commandTokens;

        VerificationKind(String artifactPrefix, List<String> commandTokens) {
            this.artifactPrefix = artifactPrefix;
            this.commandTokens = List.copyOf(commandTokens);
        }

        List<String> commandTokens() {
            return commandTokens;
        }

        String command() {
            return String.join(" ", commandTokens);
        }

        String markerRelativePath(String inputFingerprint) {
            if (this == EXACT_TESTS) {
                return artifactPrefix + ".attempt.properties";
            }
            return artifactPrefix + "-" + inputFingerprint + ".attempt.properties";
        }

        String logRelativePath(String inputFingerprint) {
            return artifactPrefix + "-" + inputFingerprint + ".log";
        }

        String receiptRelativePath(String inputFingerprint) {
            return artifactPrefix + "-" + inputFingerprint + ".receipt.properties";
        }
    }

    @FunctionalInterface
    interface VerificationCommandExecutor {
        int execute(List<String> command, Path workingDirectory, Path log) throws Exception;
    }

    @FunctionalInterface
    interface InputVerifier {
        String verify(Path repositoryRoot);
    }

    public record FinalAttempt(
        Path repositoryRoot,
        Path marker,
        Path report,
        Path statistics,
        Path log,
        Path receipt,
        String inputFingerprint,
        String cellSetSha256,
        String hypothesisSetSha256,
        String exactReceiptRelativePath,
        String exactReceiptSha256,
        String fullBuildReceiptRelativePath,
        String fullBuildReceiptSha256
    ) {
    }

    public record VerificationReceipt(
        VerificationKind kind,
        String inputFingerprint,
        String command,
        String receiptRelativePath,
        String receiptSha256,
        Path log,
        String logSha256
    ) {
    }

    public record FinalReceipt(
        String inputFingerprint,
        String cellSetSha256,
        String hypothesisSetSha256,
        Path report,
        String reportSha256,
        Path statistics,
        String statisticsSha256,
        Path log,
        String logSha256
    ) {
    }

    public record FinalEvidence(
        String inputFingerprint,
        VerificationReceipt exactTests,
        VerificationReceipt fullBuild,
        String reportSha256,
        String statisticsSha256
    ) {
    }
}
