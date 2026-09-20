package ru.homyakin.seeker.game.battle.simulation;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

/** Runs and records the immutable exact-test or full-build prerequisite for V3 acceptance. */
public final class Step12V3VerificationOrchestrator {
    private Step12V3VerificationOrchestrator() {
    }

    /** Entry point kept outside Maven so a nested clean build cannot delete its caller's reports. */
    public static void main(String[] args) {
        if (!"1".equals(System.getenv("STEP12_V3_VERIFICATION_LAUNCHER"))) {
            throw new IllegalStateException("Use game/run-step12-v3-verification.sh");
        }
        if (args.length != 1) {
            throw new IllegalArgumentException("Expected exactly one verification kind");
        }
        final var kind = Step12V3AcceptanceLedger.VerificationKind.valueOf(
            args[0].toUpperCase(Locale.ROOT)
        );
        final var ledger = Step12V3AcceptanceLedger.fromTestClasses(
            Step12V3VerificationOrchestrator.class
        );
        final var receipt = ledger.runVerification(
            kind,
            (command, workingDirectory, log) -> execute(kind, command, workingDirectory, log)
        );
        System.out.println("Verification receipt: " + receipt);
    }

    private static int execute(
        Step12V3AcceptanceLedger.VerificationKind kind,
        List<String> logicalCommand,
        Path workingDirectory,
        Path log
    )
        throws Exception {
        final var command = new ArrayList<>(logicalCommand);
        command.set(0, mavenExecutable());
        final var process = new ProcessBuilder(command)
            .directory(workingDirectory.toFile())
            .redirectErrorStream(true)
            .redirectOutput(ProcessBuilder.Redirect.appendTo(log.toFile()))
            .start();
        final int exitCode = process.waitFor();
        if (exitCode == 0 && kind == Step12V3AcceptanceLedger.VerificationKind.EXACT_TESTS) {
            verifyExactReports(workingDirectory.resolve("target/surefire-reports"));
            Files.writeString(
                log,
                "Verified exact test reports: "
                    + Step12V3AcceptanceLedger.EXACT_TEST_CLASSES.size()
                    + System.lineSeparator(),
                StandardOpenOption.APPEND
            );
        }
        return exitCode;
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
        for (final var testClass : Step12V3AcceptanceLedger.EXACT_TEST_CLASSES) {
            final var matches = reports.stream()
                .filter(path -> path.getFileName().toString().endsWith("." + testClass + ".xml"))
                .toList();
            if (matches.size() != 1) {
                throw new IllegalStateException(
                    "Expected one fresh Surefire report for " + testClass + ", got " + matches.size()
                );
            }
            verifySuccessfulTestSuite(testClass, matches.getFirst());
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
}
