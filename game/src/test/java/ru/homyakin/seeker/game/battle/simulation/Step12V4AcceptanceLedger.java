package ru.homyakin.seeker.game.battle.simulation;

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

/** Persistent one-use ledger for acceptance-protocol V4 applied to the gameplay V3 candidate. */
public final class Step12V4AcceptanceLedger {
    static final String MANIFEST_RELATIVE_PATH = "documentation/raid-redesign-step-12-v4-inputs.sha256";
    static final List<String> EXACT_TEST_CLASSES = List.of(
        "BattleEventJsonTest",
        "BattleLocalizationTest",
        "BattleSkillInitSnapshotVersionTest",
        "CombatSimulatorTest",
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
        "Step12CatalogAcceptanceTest",
        "Step12CausalObservationRunnerTest",
        "Step12CausalStatisticsTest",
        "Step12CombatMatrixTest",
        "Step12InputManifestTest",
        "Step12MixedBuildFixturesTest",
        "Step12OneShotBattleRandomTest",
        "Step12V2SimulationFixturesTest",
        "Step12V3AcceptanceLedgerTest",
        "Step12V3CausalSimulatorTest",
        "Step12V3FormulaWiringTest",
        "Step12V3ObservationCorrectionTest",
        "Step12V4AcceptanceLedgerTest",
        "Step12V4InputManifestTest",
        "VersionedAttemptThreatTest",
        "VersionedBattleBoundaryTest",
        "VersionedExistingSkillsBattleTest",
        "VersionedSkillIntegrationTest",
        "VersionedTeamSkillStateTest",
        "VersionedTurnExecutionTest",
        "WorldRaidPersonageSkillCompatibilityTest"
    );

    private static final String SCHEMA = "step12-v4-acceptance-v1";
    private static final List<String> OUTCOME_BUILD_CODES = List.of(
        "GUARDIAN",
        "BRUISER",
        "SKIRMISHER",
        "ASSASSIN",
        "RANGER_PHYSICAL",
        "RANGER_MAGICAL",
        "BREAKER_PHYSICAL",
        "BREAKER_MAGICAL",
        "TACTICIAN_PHYSICAL",
        "TACTICIAN_MAGICAL"
    );
    private static final List<String> V3_MATCHUP_CODES = List.of(
        "БЛАГО-ГРОМИЛА_V3",
        "БЛАГО-ЗАСТРЕЛЬЩИК_V3",
        "БЛАГО-УБИЙЦА_V3",
        "БЛАГО-ДАЛЬНОБОЕЦ_V3",
        "БЛАГО-РАЗРУШИТЕЛЬ_V3",
        "БЛАГО-ТАКТИК_V3"
    );
    private static final List<Integer> CONTROL_LEVELS = List.of(0, 3, 6, 10, 20);
    private static final List<Integer> MIRROR_PARTY_SIZES = List.of(1, 3, 7);
    private static final List<Integer> GUARDIAN_PARTY_SIZES = List.of(3, 7);
    private static final List<String> FACTORIAL_WORLD_CODES = List.of("++", "-+", "+-", "--");
    private static final long FNV_OFFSET_BASIS = 0xcbf29ce484222325L;
    private static final long FNV_PRIME = 0x100000001b3L;
    private static final Set<String> RECEIPT_KEYS = Set.of(
        "schema",
        "status",
        "kind",
        "repositoryRoot",
        "rootSeed",
        "iterations",
        "maxRounds",
        "cellSetSha256",
        "inputFingerprint",
        "report",
        "reportSha256"
    );
    private static final Set<String> VERIFICATION_RECEIPT_KEYS = Set.of(
        "schema",
        "status",
        "kind",
        "repositoryRoot",
        "inputFingerprint",
        "command",
        "commandSha256",
        "log",
        "exitCode",
        "logSha256"
    );

    private final Path repositoryRoot;

    Step12V4AcceptanceLedger(Path repositoryRoot) {
        this.repositoryRoot = canonicalDirectory(repositoryRoot);
        if (!Files.isRegularFile(this.repositoryRoot.resolve("game/pom.xml"))
            || !Files.isDirectory(this.repositoryRoot.resolve("documentation"))) {
            throw new IllegalArgumentException(
                "Step 12 repository root must contain game/pom.xml and documentation: " + this.repositoryRoot
            );
        }
    }

    /** Resolves the actual checkout from compiled test classes, never from a CLI-supplied root. */
    public static Step12V4AcceptanceLedger fromTestClasses(Class<?> anchor) {
        Objects.requireNonNull(anchor, "anchor");
        final Path codeLocation;
        try {
            codeLocation = Path.of(
                anchor.getProtectionDomain().getCodeSource().getLocation().toURI()
            );
        } catch (URISyntaxException | NullPointerException exception) {
            throw new IllegalStateException("Cannot resolve test-classes location", exception);
        }
        return new Step12V4AcceptanceLedger(findRepositoryRoot(codeLocation));
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
        throw new IllegalStateException(
            "Cannot find repository root above test-classes location: " + codeLocation
        );
    }

    /**
     * Validates a frozen acceptance request and atomically burns its one-use marker before any battle starts.
     */
    public Attempt begin(
        RunKind kind,
        long rootSeed,
        int iterations,
        int maxRounds,
        Collection<String> cellDescriptors,
        String explicitlyConfiguredOutput
    ) {
        Objects.requireNonNull(kind, "kind");
        validateRunParameters(kind, rootSeed, iterations, maxRounds);
        final var report = resolve(kind.reportRelativePath());
        validateExplicitOutput(report, explicitlyConfiguredOutput);
        final var manifest = resolve(MANIFEST_RELATIVE_PATH);
        final var inputFingerprint = Step12V4InputManifest.verifyAndFingerprint(manifest, repositoryRoot);
        verifyVerificationReceipt(VerificationKind.EXACT_TESTS, inputFingerprint);
        if (kind == RunKind.CAUSAL_PREFLIGHT) {
            requireSuccessfulRun(RunKind.OUTCOME_PREFLIGHT, inputFingerprint);
        }
        if (kind.finalRun()) {
            verifyVerificationReceipt(VerificationKind.FULL_BUILD, inputFingerprint);
            requireSuccessfulPreflights(inputFingerprint);
        }
        if (kind == RunKind.CAUSAL_FINAL) {
            requireSuccessfulRun(RunKind.OUTCOME_FINAL, inputFingerprint);
        }
        final var canonicalCells = canonicalCellDescriptors(cellDescriptors);
        if (!canonicalCells.equals(kind.canonicalCellDescriptors())) {
            throw new IllegalArgumentException(
                "Invalid " + kind + " acceptance cell set; the complete canonical set is required"
            );
        }
        final var cellSetFingerprint = fingerprintCanonicalCells(canonicalCells);
        final var marker = resolve(kind.markerRelativePath());
        final var receipt = resolve(kind.receiptRelativePath());
        rejectPreexistingArtifact(report, "report");
        rejectPreexistingArtifact(receipt, "receipt");

        final var attempt = new Attempt(
            kind,
            repositoryRoot,
            marker,
            report,
            receipt,
            rootSeed,
            iterations,
            maxRounds,
            cellSetFingerprint,
            inputFingerprint
        );
        writeNew(marker, encode(attemptFields(attempt, "STARTED")), "attempt marker");
        return attempt;
    }

    /** Writes the fixed acceptance report without permitting replacement of an earlier artifact. */
    public void writeReport(Attempt attempt, String markdown) {
        Objects.requireNonNull(markdown, "markdown");
        validateAttempt(attempt);
        verifyAttemptMarker(attempt);
        writeNew(attempt.report(), markdown, "acceptance report");
    }

    /** Creates a success receipt after the caller has enforced every acceptance assertion. */
    public Receipt complete(Attempt attempt) {
        validateAttempt(attempt);
        verifyAttemptMarker(attempt);
        if (!Files.isRegularFile(attempt.report())) {
            throw new IllegalStateException("Acceptance report is missing: " + attempt.report());
        }
        final var currentFingerprint = Step12V4InputManifest.verifyAndFingerprint(
            resolve(MANIFEST_RELATIVE_PATH),
            repositoryRoot
        );
        if (!currentFingerprint.equals(attempt.inputFingerprint())) {
            throw new IllegalStateException("Acceptance inputs changed after the attempt started");
        }
        final var reportSha256 = fileSha256(attempt.report());
        final var receiptFields = attemptFields(attempt, "SUCCESS");
        receiptFields.put("reportSha256", reportSha256);
        writeNew(attempt.receipt(), encode(receiptFields), "success receipt");
        return new Receipt(
            attempt.kind(),
            attempt.rootSeed(),
            attempt.iterations(),
            attempt.maxRounds(),
            attempt.cellSetFingerprint(),
            attempt.inputFingerprint(),
            attempt.report(),
            reportSha256
        );
    }

    /** Verifies that both final layers succeeded on this exact checkout snapshot. */
    public FinalEvidence verifyFinalEvidence() {
        final var currentFingerprint = Step12V4InputManifest.verifyAndFingerprint(
            resolve(MANIFEST_RELATIVE_PATH),
            repositoryRoot
        );
        final var exactTests = verifyVerificationReceipt(
            VerificationKind.EXACT_TESTS,
            currentFingerprint
        );
        final var fullBuild = verifyVerificationReceipt(
            VerificationKind.FULL_BUILD,
            currentFingerprint
        );
        requireSuccessfulPreflights(currentFingerprint);
        final var outcome = verifyReceipt(RunKind.OUTCOME_FINAL);
        final var causal = verifyReceipt(RunKind.CAUSAL_FINAL);
        if (!outcome.inputFingerprint().equals(causal.inputFingerprint())) {
            throw new IllegalStateException("V4 final receipts use different input fingerprints");
        }
        if (!currentFingerprint.equals(outcome.inputFingerprint())) {
            throw new IllegalStateException("V4 final evidence belongs to a different input snapshot");
        }
        return new FinalEvidence(currentFingerprint, exactTests, fullBuild, outcome, causal);
    }

    VerificationReceipt runVerification(
        VerificationKind kind,
        VerificationCommandExecutor executor
    ) {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(executor, "executor");
        final var inputFingerprint = Step12V4InputManifest.verifyAndFingerprint(
            resolve(MANIFEST_RELATIVE_PATH),
            repositoryRoot
        );
        if (kind == VerificationKind.FULL_BUILD) {
            requireSuccessfulPreflights(inputFingerprint);
        }
        if (kind == VerificationKind.EXACT_TESTS) {
            rejectExistingExactTestAttempt();
        }
        final var marker = resolve(kind.markerRelativePath(inputFingerprint));
        final var log = resolve(kind.logRelativePath(inputFingerprint));
        final var receipt = resolve(kind.receiptRelativePath(inputFingerprint));
        rejectPreexistingArtifact(log, "verification log");
        rejectPreexistingArtifact(receipt, "verification receipt");
        final var command = kind.command();
        final var markerFields = verificationFields(kind, inputFingerprint, command, "STARTED");
        writeNew(marker, encode(markerFields), "verification attempt marker");
        writeNew(log, "", "verification log");

        final int exitCode;
        try {
            exitCode = executor.execute(kind.commandTokens(), repositoryRoot.resolve("game"), log);
        } catch (Exception exception) {
            throw new IllegalStateException("V4 verification command failed to execute: " + kind, exception);
        }
        if (exitCode != 0) {
            throw new IllegalStateException(
                "V4 verification command failed with exit code " + exitCode + ": " + kind
            );
        }
        final var currentFingerprint = Step12V4InputManifest.verifyAndFingerprint(
            resolve(MANIFEST_RELATIVE_PATH),
            repositoryRoot
        );
        if (!currentFingerprint.equals(inputFingerprint)) {
            throw new IllegalStateException("V4 verification inputs changed while the command was running");
        }
        final var logSha256 = fileSha256(log);
        final var receiptFields = verificationFields(kind, inputFingerprint, command, "SUCCESS");
        receiptFields.put("exitCode", "0");
        receiptFields.put("logSha256", logSha256);
        writeNew(receipt, encode(receiptFields), "verification success receipt");
        return new VerificationReceipt(kind, inputFingerprint, command, log, logSha256);
    }

    private void rejectExistingExactTestAttempt() {
        final var documentation = resolve("documentation");
        final Path existing;
        try (var paths = Files.list(documentation)) {
            existing = paths
                .filter(Files::isRegularFile)
                .filter(path -> {
                    final var name = path.getFileName().toString();
                    return name.startsWith("raid-redesign-step-12-v4-exact-tests")
                        && name.endsWith(".attempt.properties");
                })
                .findFirst()
                .orElse(null);
        } catch (IOException exception) {
            throw new IllegalStateException(
                "Cannot inspect V4 exact-test attempt markers: " + documentation,
                exception
            );
        }
        if (existing != null) {
            throw new IllegalStateException(
                "V4 exact tests already have a one-use attempt marker: " + existing
            );
        }
    }

    /** Stable SHA-256 of the exact selected acceptance cells, independent of iteration order. */
    public static String cellSetFingerprint(Collection<String> descriptors) {
        return fingerprintCanonicalCells(canonicalCellDescriptors(descriptors));
    }

    private static List<String> canonicalCellDescriptors(Collection<String> descriptors) {
        Objects.requireNonNull(descriptors, "descriptors");
        if (descriptors.isEmpty()) {
            throw new IllegalArgumentException("Acceptance cell set must not be empty");
        }
        final var sorted = new ArrayList<String>(descriptors.size());
        for (final var descriptor : descriptors) {
            if (descriptor == null || descriptor.isBlank() || descriptor.contains("\n") || descriptor.contains("\r")) {
                throw new IllegalArgumentException("Acceptance cell descriptors must be non-blank single lines");
            }
            sorted.add(descriptor);
        }
        sorted.sort(String::compareTo);
        if (Set.copyOf(sorted).size() != sorted.size()) {
            throw new IllegalArgumentException("Acceptance cell descriptors must be unique");
        }
        return List.copyOf(sorted);
    }

    private static String fingerprintCanonicalCells(List<String> canonicalDescriptors) {
        return sha256(String.join("\n", canonicalDescriptors) + "\n");
    }

    private void requireSuccessfulPreflights(String inputFingerprint) {
        final var outcome = requireSuccessfulRun(RunKind.OUTCOME_PREFLIGHT, inputFingerprint);
        final var causal = requireSuccessfulRun(RunKind.CAUSAL_PREFLIGHT, inputFingerprint);
        if (!outcome.inputFingerprint().equals(causal.inputFingerprint())) {
            throw new IllegalStateException("V4 preflight receipts use different input fingerprints");
        }
    }

    private Receipt requireSuccessfulRun(RunKind kind, String inputFingerprint) {
        final var receipt = verifyReceipt(kind);
        if (!inputFingerprint.equals(receipt.inputFingerprint())) {
            throw new IllegalStateException(kind + " belongs to a different V4 input snapshot");
        }
        return receipt;
    }

    private VerificationReceipt verifyVerificationReceipt(
        VerificationKind kind,
        String inputFingerprint
    ) {
        final var receiptPath = resolve(kind.receiptRelativePath(inputFingerprint));
        final var fields = readFields(receiptPath, VERIFICATION_RECEIPT_KEYS);
        final var command = kind.command();
        requireField(fields, "schema", SCHEMA, receiptPath);
        requireField(fields, "status", "SUCCESS", receiptPath);
        requireField(fields, "kind", kind.name(), receiptPath);
        requireField(fields, "repositoryRoot", repositoryRoot.toString(), receiptPath);
        requireField(fields, "inputFingerprint", inputFingerprint, receiptPath);
        requireField(fields, "command", command, receiptPath);
        requireField(fields, "commandSha256", sha256(command), receiptPath);
        requireField(fields, "log", kind.logRelativePath(inputFingerprint), receiptPath);
        requireField(fields, "exitCode", "0", receiptPath);
        requireSha256(fields.get("logSha256"), "logSha256", receiptPath);

        final var expectedMarker = new LinkedHashMap<>(fields);
        expectedMarker.remove("exitCode");
        expectedMarker.remove("logSha256");
        expectedMarker.put("status", "STARTED");
        final var markerPath = resolve(kind.markerRelativePath(inputFingerprint));
        if (!readFields(markerPath, expectedMarker.keySet()).equals(expectedMarker)) {
            throw new IllegalStateException(
                "Verification receipt marker is missing or inconsistent: " + markerPath
            );
        }
        final var log = resolve(kind.logRelativePath(inputFingerprint));
        if (!Files.isRegularFile(log)) {
            throw new IllegalStateException("Verification log is missing: " + log);
        }
        requireField(fields, "logSha256", fileSha256(log), receiptPath);
        return new VerificationReceipt(kind, inputFingerprint, command, log, fields.get("logSha256"));
    }

    private LinkedHashMap<String, String> verificationFields(
        VerificationKind kind,
        String inputFingerprint,
        String command,
        String status
    ) {
        final var fields = new LinkedHashMap<String, String>();
        fields.put("schema", SCHEMA);
        fields.put("status", status);
        fields.put("kind", kind.name());
        fields.put("repositoryRoot", repositoryRoot.toString());
        fields.put("inputFingerprint", inputFingerprint);
        fields.put("command", command);
        fields.put("commandSha256", sha256(command));
        fields.put("log", kind.logRelativePath(inputFingerprint));
        return fields;
    }

    private Receipt verifyReceipt(RunKind kind) {
        final var receiptPath = resolve(kind.receiptRelativePath());
        final var fields = readFields(receiptPath, RECEIPT_KEYS);
        requireField(fields, "schema", SCHEMA, receiptPath);
        requireField(fields, "status", "SUCCESS", receiptPath);
        requireField(fields, "kind", kind.name(), receiptPath);
        requireField(fields, "repositoryRoot", repositoryRoot.toString(), receiptPath);
        requireField(fields, "rootSeed", Long.toString(kind.rootSeed()), receiptPath);
        requireField(fields, "iterations", Integer.toString(kind.iterations()), receiptPath);
        requireField(fields, "maxRounds", Integer.toString(kind.maxRounds()), receiptPath);
        requireField(
            fields,
            "cellSetSha256",
            kind.expectedCellSetFingerprint(),
            receiptPath
        );
        requireSha256(fields.get("inputFingerprint"), "inputFingerprint", receiptPath);
        final var expectedMarker = new LinkedHashMap<>(fields);
        expectedMarker.remove("reportSha256");
        expectedMarker.put("status", "STARTED");
        final var markerPath = resolve(kind.markerRelativePath());
        if (!readFields(markerPath, expectedMarker.keySet()).equals(expectedMarker)) {
            throw new IllegalStateException("Receipt attempt marker is missing or inconsistent: " + markerPath);
        }
        final var expectedReport = resolve(kind.reportRelativePath());
        requireField(fields, "report", kind.reportRelativePath(), receiptPath);
        if (!Files.isRegularFile(expectedReport)) {
            throw new IllegalStateException("Receipt report is missing: " + expectedReport);
        }
        final var actualReportSha256 = fileSha256(expectedReport);
        requireField(fields, "reportSha256", actualReportSha256, receiptPath);
        return new Receipt(
            kind,
            kind.rootSeed(),
            kind.iterations(),
            kind.maxRounds(),
            fields.get("cellSetSha256"),
            fields.get("inputFingerprint"),
            expectedReport,
            actualReportSha256
        );
    }

    private void validateExplicitOutput(Path report, String explicitlyConfiguredOutput) {
        if (explicitlyConfiguredOutput == null) {
            return;
        }
        if (explicitlyConfiguredOutput.isBlank()) {
            throw new IllegalArgumentException("Acceptance output must not be blank");
        }
        final var configured = Path.of(explicitlyConfiguredOutput);
        final var absolute = configured.isAbsolute()
            ? configured.toAbsolutePath().normalize()
            : repositoryRoot.resolve(configured).normalize();
        if (!absolute.equals(report)) {
            throw new IllegalArgumentException("Acceptance output is fixed at " + report);
        }
    }

    private static void validateRunParameters(
        RunKind kind,
        long rootSeed,
        int iterations,
        int maxRounds
    ) {
        final var violations = new ArrayList<String>();
        if (rootSeed != kind.rootSeed()) {
            violations.add("root seed must equal " + kind.rootSeed());
        }
        if (iterations != kind.iterations()) {
            violations.add("iterations must equal " + kind.iterations());
        }
        if (maxRounds != kind.maxRounds()) {
            violations.add("maxRounds must equal " + kind.maxRounds());
        }
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException(
                "Invalid " + kind + " ledger request: " + String.join("; ", violations)
            );
        }
    }

    private void validateAttempt(Attempt attempt) {
        Objects.requireNonNull(attempt, "attempt");
        if (!attempt.repositoryRoot().equals(repositoryRoot)
            || !attempt.marker().equals(resolve(attempt.kind().markerRelativePath()))
            || !attempt.report().equals(resolve(attempt.kind().reportRelativePath()))
            || !attempt.receipt().equals(resolve(attempt.kind().receiptRelativePath()))) {
            throw new IllegalArgumentException("Acceptance attempt does not belong to this repository ledger");
        }
    }

    private void verifyAttemptMarker(Attempt attempt) {
        final var expected = attemptFields(attempt, "STARTED");
        final var actual = readFields(attempt.marker(), expected.keySet());
        if (!actual.equals(expected)) {
            throw new IllegalStateException("Acceptance attempt marker is corrupt: " + attempt.marker());
        }
    }

    private LinkedHashMap<String, String> attemptFields(Attempt attempt, String status) {
        final var fields = new LinkedHashMap<String, String>();
        fields.put("schema", SCHEMA);
        fields.put("status", status);
        fields.put("kind", attempt.kind().name());
        fields.put("repositoryRoot", repositoryRoot.toString());
        fields.put("rootSeed", Long.toString(attempt.rootSeed()));
        fields.put("iterations", Integer.toString(attempt.iterations()));
        fields.put("maxRounds", Integer.toString(attempt.maxRounds()));
        fields.put("cellSetSha256", attempt.cellSetFingerprint());
        fields.put("inputFingerprint", attempt.inputFingerprint());
        fields.put("report", attempt.kind().reportRelativePath());
        return fields;
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
        final var text = new StringBuilder();
        fields.forEach((key, value) -> {
            if (key.contains("=") || key.contains("\n") || value.contains("\n") || value.contains("\r")) {
                throw new IllegalArgumentException("Acceptance ledger fields must be single-line key/value pairs");
            }
            text.append(key).append('=').append(value).append('\n');
        });
        return text.toString();
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
                "One-use " + artifactName + " already exists; this run cannot be repeated: " + path,
                exception
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot create " + artifactName + ": " + path, exception);
        }
    }

    private static void rejectPreexistingArtifact(Path path, String artifactName) {
        if (Files.exists(path)) {
            throw new IllegalStateException("Preexisting acceptance " + artifactName + " blocks the run: " + path);
        }
    }

    private static void requireField(
        Map<String, String> fields,
        String key,
        String expected,
        Path receipt
    ) {
        if (!expected.equals(fields.get(key))) {
            throw new IllegalStateException("Invalid " + key + " in acceptance receipt: " + receipt);
        }
    }

    private static void requireSha256(String value, String key, Path receipt) {
        if (value == null || !value.matches("[0-9a-f]{64}")) {
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
        try {
            return sha256(Files.readAllBytes(path));
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot hash acceptance artifact: " + path, exception);
        }
    }

    private static String sha256(String value) {
        return sha256(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static List<String> outcomeCellDescriptors(long rootSeed) {
        final var descriptors = new ArrayList<String>();
        for (final var build : OUTCOME_BUILD_CODES) {
            for (final int partySize : MIRROR_PARTY_SIZES) {
                for (final int level : CONTROL_LEVELS) {
                    final var code = "MIRROR_%s_N%d_L%d".formatted(build, partySize, level);
                    descriptors.add("%s|MIRROR|%d|%d|%d|MirrorFixture".formatted(
                        code,
                        partySize,
                        level,
                        acceptanceCellSeed(rootSeed, code)
                    ));
                }
            }
        }
        for (final var matchup : V3_MATCHUP_CODES) {
            for (final int level : CONTROL_LEVELS) {
                final var code = matchup + "_N3_L" + level;
                descriptors.add("%s|FAVORABLE|3|%d|%d|V3FavorableFixture".formatted(
                    code,
                    level,
                    acceptanceCellSeed(rootSeed, code)
                ));
            }
        }
        return List.copyOf(descriptors);
    }

    private static List<String> causalCellDescriptors(long rootSeed) {
        final var descriptors = new ArrayList<String>();
        for (final var matchup : V3_MATCHUP_CODES) {
            for (final int level : CONTROL_LEVELS) {
                final var code = matchup + "_ПРИЧИНА_L" + level;
                final long seed = acceptanceCellSeed(rootSeed, code);
                for (final var world : FACTORIAL_WORLD_CODES) {
                    descriptors.add("FACTORIAL|%s|%s|%d|%d".formatted(code, world, level, seed));
                }
            }
        }
        for (final int partySize : GUARDIAN_PARTY_SIZES) {
            for (final int level : CONTROL_LEVELS) {
                final var code = "СТРАЖ_V3_ПРИЧИНА_N%d_L%d".formatted(partySize, level);
                descriptors.add("GUARDIAN|%s|%d|%d|%d".formatted(
                    code,
                    partySize,
                    level,
                    acceptanceCellSeed(rootSeed, code)
                ));
            }
        }
        return List.copyOf(descriptors);
    }

    private static long acceptanceCellSeed(long rootSeed, String cellCode) {
        var hash = FNV_OFFSET_BASIS;
        for (int index = 0; index < cellCode.length(); index++) {
            hash ^= cellCode.charAt(index);
            hash *= FNV_PRIME;
        }
        var mixed = rootSeed ^ hash;
        mixed = (mixed ^ mixed >>> 30) * 0xbf58476d1ce4e5b9L;
        mixed = (mixed ^ mixed >>> 27) * 0x94d049bb133111ebL;
        return mixed ^ mixed >>> 31;
    }

    /** Fixed acceptance-run identities and their immutable artifacts. */
    public enum RunKind {
        OUTCOME_PREFLIGHT(
            2_026_092_203L,
            2_000,
            10_000,
            "documentation/raid-redesign-step-12-v4-outcome-preflight.md",
            outcomeCellDescriptors(2_026_092_203L)
        ),
        CAUSAL_PREFLIGHT(
            2_026_092_204L,
            2_000,
            10_000,
            "documentation/raid-redesign-step-12-v4-causal-preflight.md",
            causalCellDescriptors(2_026_092_204L)
        ),
        OUTCOME_FINAL(
            2_026_092_302L,
            10_000,
            10_000,
            "documentation/raid-redesign-step-12-v4-outcome-final.md",
            outcomeCellDescriptors(2_026_092_302L)
        ),
        CAUSAL_FINAL(
            2_026_092_302L,
            10_000,
            10_000,
            "documentation/raid-redesign-step-12-v4-causal-final.md",
            causalCellDescriptors(2_026_092_302L)
        );

        private final long rootSeed;
        private final int iterations;
        private final int maxRounds;
        private final String reportRelativePath;
        private final List<String> canonicalCellDescriptors;
        private final String expectedCellSetFingerprint;

        RunKind(
            long rootSeed,
            int iterations,
            int maxRounds,
            String reportRelativePath,
            List<String> canonicalCellDescriptors
        ) {
            this.rootSeed = rootSeed;
            this.iterations = iterations;
            this.maxRounds = maxRounds;
            this.reportRelativePath = reportRelativePath;
            this.canonicalCellDescriptors = Step12V4AcceptanceLedger.canonicalCellDescriptors(
                canonicalCellDescriptors
            );
            this.expectedCellSetFingerprint = fingerprintCanonicalCells(this.canonicalCellDescriptors);
        }

        public long rootSeed() {
            return rootSeed;
        }

        public int iterations() {
            return iterations;
        }

        public int maxRounds() {
            return maxRounds;
        }

        public String reportRelativePath() {
            return reportRelativePath;
        }

        public String expectedCellSetFingerprint() {
            return expectedCellSetFingerprint;
        }

        List<String> canonicalCellDescriptors() {
            return canonicalCellDescriptors;
        }

        String markerRelativePath() {
            return reportRelativePath.replace(".md", ".attempt.properties");
        }

        String receiptRelativePath() {
            return reportRelativePath.replace(".md", ".receipt.properties");
        }

        boolean finalRun() {
            return this == OUTCOME_FINAL || this == CAUSAL_FINAL;
        }
    }

    public enum VerificationKind {
        EXACT_TESTS(
            "documentation/raid-redesign-step-12-v4-exact-tests",
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
            "documentation/raid-redesign-step-12-v4-full-build",
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

    /** A marker-backed run that has already consumed its one-use root. */
    public static final class Attempt {
        private final RunKind kind;
        private final Path repositoryRoot;
        private final Path marker;
        private final Path report;
        private final Path receipt;
        private final long rootSeed;
        private final int iterations;
        private final int maxRounds;
        private final String cellSetFingerprint;
        private final String inputFingerprint;

        private Attempt(
            RunKind kind,
            Path repositoryRoot,
            Path marker,
            Path report,
            Path receipt,
            long rootSeed,
            int iterations,
            int maxRounds,
            String cellSetFingerprint,
            String inputFingerprint
        ) {
            this.kind = kind;
            this.repositoryRoot = repositoryRoot;
            this.marker = marker;
            this.report = report;
            this.receipt = receipt;
            this.rootSeed = rootSeed;
            this.iterations = iterations;
            this.maxRounds = maxRounds;
            this.cellSetFingerprint = cellSetFingerprint;
            this.inputFingerprint = inputFingerprint;
        }

        public RunKind kind() {
            return kind;
        }

        Path repositoryRoot() {
            return repositoryRoot;
        }

        public Path marker() {
            return marker;
        }

        public Path report() {
            return report;
        }

        public Path receipt() {
            return receipt;
        }

        public long rootSeed() {
            return rootSeed;
        }

        public int iterations() {
            return iterations;
        }

        public int maxRounds() {
            return maxRounds;
        }

        public String cellSetFingerprint() {
            return cellSetFingerprint;
        }

        public String inputFingerprint() {
            return inputFingerprint;
        }
    }

    /** Verified success metadata used as a prerequisite for final execution. */
    public record Receipt(
        RunKind kind,
        long rootSeed,
        int iterations,
        int maxRounds,
        String cellSetFingerprint,
        String inputFingerprint,
        Path report,
        String reportSha256
    ) {
    }

    public record VerificationReceipt(
        VerificationKind kind,
        String inputFingerprint,
        String command,
        Path log,
        String logSha256
    ) {
    }

    /** Complete evidence for the accepted outcome and causal layers. */
    public record FinalEvidence(
        String inputFingerprint,
        VerificationReceipt exactTests,
        VerificationReceipt fullBuild,
        Receipt outcome,
        Receipt causal
    ) {
    }
}
