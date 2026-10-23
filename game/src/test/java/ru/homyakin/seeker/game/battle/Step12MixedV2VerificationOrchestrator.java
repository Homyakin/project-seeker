package ru.homyakin.seeker.game.battle;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

/** Runs the immutable exact, full-build and official stages for mixed-build acceptance V2. */
public final class Step12MixedV2VerificationOrchestrator {
    private static final String LAUNCHER_ENVIRONMENT = "STEP12_MIXED_V2_VERIFICATION_LAUNCHER";

    private Step12MixedV2VerificationOrchestrator() {
    }

    /** Kept outside Maven so a nested clean invocation cannot remove its running caller. */
    public static void main(String[] args) {
        if (!"1".equals(System.getenv(LAUNCHER_ENVIRONMENT))) {
            throw new IllegalStateException("Use game/run-step12-mixed-v2-verification.sh");
        }
        if (args.length != 1) {
            throw new IllegalArgumentException("Expected exactly one stage: EXACT_TESTS, FULL_BUILD or FINAL");
        }
        final var stage = Stage.valueOf(args[0].toUpperCase(Locale.ROOT));
        final var ledger = Step12MixedV2AcceptanceLedger.fromTestClasses(
            Step12MixedV2VerificationOrchestrator.class
        );
        if (stage == Stage.FINAL) {
            runFinal(ledger);
            return;
        }
        final var kind = Step12MixedV2AcceptanceLedger.VerificationKind.valueOf(stage.name());
        final var receipt = ledger.runVerification(
            kind,
            (command, workingDirectory, log) -> execute(kind, command, workingDirectory, log)
        );
        System.out.println("Verification receipt: " + receipt.receiptRelativePath());
    }

    private static void runFinal(Step12MixedV2AcceptanceLedger ledger) {
        final var attempt = ledger.beginFinal(
            Step12MixedV2AcceptanceLedger.FINAL_ROOT_SEED,
            Step12MixedV2AcceptanceLedger.FINAL_ITERATIONS,
            Step12MixedV2AcceptanceLedger.FINAL_MAX_ROUNDS,
            Step12MixedV2AcceptanceLedger.FINAL_WORKERS,
            Step12MixedV2AcceptanceLedger.expectedFinalCellDescriptors(),
            Step12MixedV2AcceptanceLedger.expectedFinalHypothesisDescriptors(),
            Step12MixedV2AcceptanceLedger.FINAL_REPORT_RELATIVE_PATH,
            Step12MixedV2AcceptanceLedger.FINAL_STATISTICS_RELATIVE_PATH
        );
        final int exitCode;
        try {
            exitCode = executeProcess(
                Step12MixedV2AcceptanceLedger.finalCommandTokens(),
                ledger.repositoryRoot().resolve("game"),
                attempt.log()
            );
        } catch (Exception exception) {
            throw new IllegalStateException("Official mixed-build command failed to execute", exception);
        }
        if (exitCode != 0) {
            throw new IllegalStateException(
                "Official mixed-build run was not accepted (exit code " + exitCode
                    + "); its seed marker remains consumed and no success receipt was created"
            );
        }
        final var receipt = ledger.completeFinal(attempt, true);
        final var evidence = ledger.verifyFinalEvidence();
        System.out.println("Final receipt: " + receipt);
        System.out.println("Verified final evidence: " + evidence);
    }

    private static int execute(
        Step12MixedV2AcceptanceLedger.VerificationKind kind,
        List<String> command,
        Path workingDirectory,
        Path log
    ) throws Exception {
        final int exitCode = executeProcess(command, workingDirectory, log);
        if (exitCode == 0
            && kind == Step12MixedV2AcceptanceLedger.VerificationKind.EXACT_TESTS) {
            verifyExactReports(workingDirectory.resolve("target/surefire-reports"));
            Files.writeString(
                log,
                "Verified exact test reports: "
                    + Step12MixedV2AcceptanceLedger.EXACT_TEST_CLASSES.size()
                    + System.lineSeparator(),
                StandardOpenOption.APPEND
            );
        }
        return exitCode;
    }

    private static int executeProcess(
        List<String> logicalCommand,
        Path workingDirectory,
        Path log
    ) throws Exception {
        final var command = new ArrayList<>(logicalCommand);
        command.set(0, mavenExecutable());
        writeLauncherProvenance(command, log);
        final var process = new ProcessBuilder(command)
            .directory(workingDirectory.toFile())
            .redirectErrorStream(true)
            .redirectOutput(ProcessBuilder.Redirect.appendTo(log.toFile()))
            .start();
        return process.waitFor();
    }

    private static void writeLauncherProvenance(List<String> command, Path log) throws Exception {
        final var javaExecutable = Path.of(System.getProperty("java.home"), "bin", "java");
        final var mavenExecutable = Path.of(command.getFirst());
        final var mockitoAgentValue = System.getenv("STEP12_MIXED_V2_MOCKITO_AGENT");
        if (mockitoAgentValue == null || mockitoAgentValue.isBlank()) {
            throw new IllegalStateException("Launcher did not declare the Mockito agent");
        }
        final var mockitoAgent = Path.of(mockitoAgentValue);
        final var provenance = new StringBuilder()
            .append("Verification launcher provenance\n")
            .append("java=").append(javaExecutable.toRealPath()).append('\n')
            .append("javaSha256=").append(Step12MixedV2InputManifest.fileSha256(javaExecutable)).append('\n')
            .append("maven=").append(mavenExecutable.toRealPath()).append('\n')
            .append("mavenSha256=").append(Step12MixedV2InputManifest.fileSha256(mavenExecutable)).append('\n')
            .append("mockitoAgent=").append(mockitoAgent.toRealPath()).append('\n')
            .append("mockitoAgentSha256=")
            .append(Step12MixedV2InputManifest.fileSha256(mockitoAgent)).append('\n')
            .append("javaToolOptions=").append(System.getenv("JAVA_TOOL_OPTIONS")).append('\n')
            .append("command=").append(String.join(" ", command)).append("\n\n")
            .toString();
        Files.writeString(log, provenance, StandardOpenOption.APPEND);
    }

    static void verifyExactReports(Path reportsDirectory) throws Exception {
        if (!Files.isDirectory(reportsDirectory)) {
            throw new IllegalStateException("Surefire reports directory is missing: " + reportsDirectory);
        }
        final List<Path> reports;
        try (var paths = Files.list(reportsDirectory)) {
            reports = paths
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().startsWith("TEST-"))
                .filter(path -> path.getFileName().toString().endsWith(".xml"))
                .toList();
        }
        final var matchedReports = new HashSet<Path>();
        for (final var testClass : Step12MixedV2AcceptanceLedger.EXACT_TEST_CLASSES) {
            final var matches = reports.stream()
                .filter(path -> path.getFileName().toString().endsWith("." + testClass + ".xml"))
                .toList();
            if (matches.size() != 1) {
                throw new IllegalStateException(
                    "Expected one fresh Surefire report for " + testClass + ", got " + matches.size()
                );
            }
            matchedReports.add(matches.getFirst());
            verifySuccessfulTestSuite(testClass, matches.getFirst());
        }
        if (!Set.copyOf(reports).equals(matchedReports)) {
            final var unexpected = new HashSet<>(reports);
            unexpected.removeAll(matchedReports);
            throw new IllegalStateException("Unexpected Surefire reports in exact run: " + unexpected);
        }
    }

    private static void verifySuccessfulTestSuite(String testClass, Path report) throws Exception {
        final var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        final org.w3c.dom.Element suite;
        try (InputStream input = Files.newInputStream(report)) {
            suite = factory.newDocumentBuilder().parse(input).getDocumentElement();
        }
        final int tests = Integer.parseInt(suite.getAttribute("tests"));
        final int errors = Integer.parseInt(suite.getAttribute("errors"));
        final int failures = Integer.parseInt(suite.getAttribute("failures"));
        final int skipped = Integer.parseInt(suite.getAttribute("skipped"));
        if (tests <= 0 || errors != 0 || failures != 0 || skipped != 0) {
            throw new IllegalStateException(
                "Exact test suite did not run cleanly: %s (tests=%d, errors=%d, failures=%d, skipped=%d)"
                    .formatted(testClass, tests, errors, failures, skipped)
            );
        }
    }

    private static String mavenExecutable() {
        final var mavenHome = System.getProperty("maven.home");
        if (mavenHome != null) {
            final var executable = Path.of(mavenHome, "bin", "mvn");
            if (Files.isRegularFile(executable)) {
                return executable.toString();
            }
        }
        final var environmentHome = System.getenv("MAVEN_HOME");
        if (environmentHome != null) {
            final var executable = Path.of(environmentHome, "bin", "mvn");
            if (Files.isRegularFile(executable)) {
                return executable.toString();
            }
        }
        return "mvn";
    }

    private enum Stage {
        EXACT_TESTS,
        FULL_BUILD,
        FINAL,
    }
}
