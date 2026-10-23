package ru.homyakin.seeker.game.battle;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class Step12MixedV2AcceptanceLedgerTest {
    private static final String FINGERPRINT = "a".repeat(64);
    private static final String CHANGED_FINGERPRINT = "b".repeat(64);

    @TempDir
    private Path temporaryDirectory;

    @Test
    void enforcesExactThenFullThenFinalOnOneFingerprint() throws Exception {
        final var fingerprint = new AtomicReference<>(FINGERPRINT);
        final var ledger = ledger(fingerprint);
        final var full = Step12MixedV2AcceptanceLedger.VerificationKind.FULL_BUILD;

        Assertions.assertThrows(
            IllegalStateException.class,
            () -> ledger.runVerification(full, Step12MixedV2AcceptanceLedgerTest::successfulCommand)
        );
        Assertions.assertFalse(Files.exists(temporaryDirectory.resolve(
            full.markerRelativePath(FINGERPRINT)
        )));

        verifyPrerequisites(ledger);
        final var attempt = beginFinal(ledger);

        Assertions.assertAll(
            () -> Assertions.assertTrue(Files.isRegularFile(attempt.marker())),
            () -> Assertions.assertTrue(Files.isRegularFile(attempt.log())),
            () -> Assertions.assertFalse(Files.exists(temporaryDirectory.resolve(
                Step12MixedV2AcceptanceLedger.FINAL_EXECUTION_MARKER_RELATIVE_PATH
            ))),
            () -> Assertions.assertFalse(Files.exists(attempt.report())),
            () -> Assertions.assertFalse(Files.exists(attempt.receipt()))
        );
    }

    @Test
    void directOrRepeatedBattleExecutionCannotBypassTheSeedMarker() throws Exception {
        final var ledger = ledger(new AtomicReference<>(FINGERPRINT));
        verifyPrerequisites(ledger);

        Assertions.assertThrows(IllegalStateException.class, () -> claimFinal(ledger));
        final var attempt = beginFinal(ledger);
        final var claimed = claimFinal(ledger);

        Assertions.assertAll(
            () -> Assertions.assertEquals(attempt, claimed),
            () -> Assertions.assertTrue(Files.isRegularFile(temporaryDirectory.resolve(
                Step12MixedV2AcceptanceLedger.FINAL_EXECUTION_MARKER_RELATIVE_PATH
            ))),
            () -> Assertions.assertThrows(IllegalStateException.class, () -> claimFinal(ledger))
        );
    }

    @Test
    void rejectsWrongParametersPartialSetsAndOutputBeforeBurningSeed() throws Exception {
        final var ledger = ledger(new AtomicReference<>(FINGERPRINT));
        verifyPrerequisites(ledger);
        final var cells = Step12MixedV2AcceptanceLedger.expectedFinalCellDescriptors();
        final var hypotheses = Step12MixedV2AcceptanceLedger.expectedFinalHypothesisDescriptors();

        Assertions.assertAll(
            () -> Assertions.assertThrows(IllegalArgumentException.class, () -> ledger.beginFinal(
                Step12MixedV2AcceptanceLedger.FINAL_ROOT_SEED + 1,
                Step12MixedV2AcceptanceLedger.FINAL_ITERATIONS,
                Step12MixedV2AcceptanceLedger.FINAL_MAX_ROUNDS,
                Step12MixedV2AcceptanceLedger.FINAL_WORKERS,
                cells,
                hypotheses,
                Step12MixedV2AcceptanceLedger.FINAL_REPORT_RELATIVE_PATH,
                Step12MixedV2AcceptanceLedger.FINAL_STATISTICS_RELATIVE_PATH
            )),
            () -> Assertions.assertThrows(IllegalArgumentException.class, () -> ledger.beginFinal(
                Step12MixedV2AcceptanceLedger.FINAL_ROOT_SEED,
                Step12MixedV2AcceptanceLedger.FINAL_ITERATIONS,
                Step12MixedV2AcceptanceLedger.FINAL_MAX_ROUNDS,
                Step12MixedV2AcceptanceLedger.FINAL_WORKERS,
                cells.subList(1, cells.size()),
                hypotheses,
                Step12MixedV2AcceptanceLedger.FINAL_REPORT_RELATIVE_PATH,
                Step12MixedV2AcceptanceLedger.FINAL_STATISTICS_RELATIVE_PATH
            )),
            () -> Assertions.assertThrows(IllegalArgumentException.class, () -> ledger.beginFinal(
                Step12MixedV2AcceptanceLedger.FINAL_ROOT_SEED,
                Step12MixedV2AcceptanceLedger.FINAL_ITERATIONS,
                Step12MixedV2AcceptanceLedger.FINAL_MAX_ROUNDS,
                Step12MixedV2AcceptanceLedger.FINAL_WORKERS,
                cells,
                hypotheses.subList(1, hypotheses.size()),
                Step12MixedV2AcceptanceLedger.FINAL_REPORT_RELATIVE_PATH,
                Step12MixedV2AcceptanceLedger.FINAL_STATISTICS_RELATIVE_PATH
            )),
            () -> Assertions.assertThrows(IllegalArgumentException.class, () -> ledger.beginFinal(
                Step12MixedV2AcceptanceLedger.FINAL_ROOT_SEED,
                Step12MixedV2AcceptanceLedger.FINAL_ITERATIONS,
                Step12MixedV2AcceptanceLedger.FINAL_MAX_ROUNDS,
                Step12MixedV2AcceptanceLedger.FINAL_WORKERS,
                cells,
                hypotheses,
                "target/other.md",
                Step12MixedV2AcceptanceLedger.FINAL_STATISTICS_RELATIVE_PATH
            )),
            () -> Assertions.assertThrows(IllegalArgumentException.class, () -> ledger.beginFinal(
                Step12MixedV2AcceptanceLedger.FINAL_ROOT_SEED,
                Step12MixedV2AcceptanceLedger.FINAL_ITERATIONS,
                Step12MixedV2AcceptanceLedger.FINAL_MAX_ROUNDS,
                Step12MixedV2AcceptanceLedger.FINAL_WORKERS,
                cells,
                hypotheses,
                Step12MixedV2AcceptanceLedger.FINAL_REPORT_RELATIVE_PATH,
                "target/other.tsv"
            ))
        );
        Assertions.assertFalse(Files.exists(temporaryDirectory.resolve(
            Step12MixedV2AcceptanceLedger.FINAL_MARKER_RELATIVE_PATH
        )));
    }

    @Test
    void rejectedGatesKeepReportAndMarkerButNeverCreateSuccessReceipt() throws Exception {
        final var ledger = ledger(new AtomicReference<>(FINGERPRINT));
        verifyPrerequisites(ledger);
        beginFinal(ledger);
        final var attempt = claimFinal(ledger);
        ledger.writeFinalStatistics(attempt, "exact\n");
        ledger.writeFinalReport(attempt, "# not accepted\n");

        Assertions.assertThrows(IllegalStateException.class, () -> ledger.completeFinal(attempt, false));

        Assertions.assertAll(
            () -> Assertions.assertTrue(Files.isRegularFile(attempt.marker())),
            () -> Assertions.assertTrue(Files.isRegularFile(attempt.report())),
            () -> Assertions.assertFalse(Files.exists(attempt.receipt())),
            () -> Assertions.assertThrows(IllegalStateException.class, () -> beginFinal(ledger))
        );
    }

    @Test
    void successfulReceiptBindsInputsPrerequisitesLogReportCellsAndHypotheses() throws Exception {
        final var ledger = ledger(new AtomicReference<>(FINGERPRINT));
        verifyPrerequisites(ledger);
        beginFinal(ledger);
        final var attempt = claimFinal(ledger);
        Files.writeString(attempt.log(), "official log\n", StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        ledger.writeFinalStatistics(attempt, "exact\n");
        ledger.writeFinalReport(attempt, "# accepted\n");

        final var receipt = ledger.completeFinal(attempt, true);
        final var evidence = ledger.verifyFinalEvidence();

        Assertions.assertAll(
            () -> Assertions.assertEquals(FINGERPRINT, receipt.inputFingerprint()),
            () -> Assertions.assertEquals(FINGERPRINT, evidence.inputFingerprint()),
            () -> Assertions.assertEquals(
                Step12MixedV2AcceptanceLedger.expectedCellSetSha256(),
                receipt.cellSetSha256()
            ),
            () -> Assertions.assertEquals(
                Step12MixedV2AcceptanceLedger.expectedHypothesisSetSha256(),
                receipt.hypothesisSetSha256()
            ),
            () -> Assertions.assertEquals(receipt.reportSha256(), evidence.reportSha256()),
            () -> Assertions.assertEquals(receipt.statisticsSha256(), evidence.statisticsSha256()),
            () -> Assertions.assertTrue(Files.readString(attempt.receipt()).contains("status=SUCCESS\n"))
        );
    }

    @Test
    void inputChangeAfterStartKeepsSeedSpentAndBlocksSuccess() throws Exception {
        final var fingerprint = new AtomicReference<>(FINGERPRINT);
        final var ledger = ledger(fingerprint);
        verifyPrerequisites(ledger);
        beginFinal(ledger);
        final var attempt = claimFinal(ledger);
        ledger.writeFinalStatistics(attempt, "exact\n");
        ledger.writeFinalReport(attempt, "# result\n");
        fingerprint.set(CHANGED_FINGERPRINT);

        Assertions.assertThrows(IllegalStateException.class, () -> ledger.completeFinal(attempt, true));

        Assertions.assertAll(
            () -> Assertions.assertTrue(Files.isRegularFile(attempt.marker())),
            () -> Assertions.assertFalse(Files.exists(attempt.receipt()))
        );
    }

    @Test
    void concurrentFinalBeginsAllowExactlyOneGlobalSeedMarker() throws Exception {
        final var ledger = ledger(new AtomicReference<>(FINGERPRINT));
        verifyPrerequisites(ledger);
        final Callable<Boolean> begin = () -> {
            try {
                beginFinal(ledger);
                return true;
            } catch (IllegalStateException exception) {
                return false;
            }
        };
        try (var executor = Executors.newFixedThreadPool(2)) {
            final var results = executor.invokeAll(List.of(begin, begin));
            final long successes = results.stream().filter(result -> {
                try {
                    return result.get();
                } catch (Exception exception) {
                    throw new IllegalStateException(exception);
                }
            }).count();

            Assertions.assertEquals(1, successes);
        }
    }

    @Test
    void concurrentBattleClaimsAllowExactlyOneExecutionMarker() throws Exception {
        final var ledger = ledger(new AtomicReference<>(FINGERPRINT));
        verifyPrerequisites(ledger);
        beginFinal(ledger);
        final Callable<Boolean> claim = () -> {
            try {
                claimFinal(ledger);
                return true;
            } catch (IllegalStateException exception) {
                return false;
            }
        };
        try (var executor = Executors.newFixedThreadPool(2)) {
            final var results = executor.invokeAll(List.of(claim, claim));
            final long successes = results.stream().filter(result -> {
                try {
                    return result.get();
                } catch (Exception exception) {
                    throw new IllegalStateException(exception);
                }
            }).count();

            Assertions.assertEquals(1, successes);
        }
    }

    @Test
    void finalEvidenceRejectsTamperingWithEveryBoundArtifactClass() throws Exception {
        final var ledger = ledger(new AtomicReference<>(FINGERPRINT));
        verifyPrerequisites(ledger);
        beginFinal(ledger);
        final var attempt = claimFinal(ledger);
        Files.writeString(attempt.log(), "official log\n", StandardOpenOption.APPEND);
        ledger.writeFinalStatistics(attempt, "exact\n");
        ledger.writeFinalReport(attempt, "# accepted\n");
        ledger.completeFinal(attempt, true);
        final var exactReceipt = temporaryDirectory.resolve(
            Step12MixedV2AcceptanceLedger.VerificationKind.EXACT_TESTS.receiptRelativePath(FINGERPRINT)
        );
        final var fullReceipt = temporaryDirectory.resolve(
            Step12MixedV2AcceptanceLedger.VerificationKind.FULL_BUILD.receiptRelativePath(FINGERPRINT)
        );

        for (final var path : List.of(
            attempt.marker(),
            temporaryDirectory.resolve(Step12MixedV2AcceptanceLedger.FINAL_EXECUTION_MARKER_RELATIVE_PATH),
            exactReceipt,
            fullReceipt,
            attempt.log(),
            attempt.statistics(),
            attempt.report(),
            attempt.receipt()
        )) {
            assertTamperDetectedAndRestore(ledger, path);
        }
    }

    @Test
    void canonicalSetsAndCommandPinTheOfficialProtocol() {
        final var cells = Step12MixedV2AcceptanceLedger.expectedFinalCellDescriptors();
        final var hypotheses = Step12MixedV2AcceptanceLedger.expectedFinalHypothesisDescriptors();
        final var command = Step12MixedV2AcceptanceLedger.finalCommand();

        Assertions.assertAll(
            () -> Assertions.assertEquals(70, cells.size()),
            () -> Assertions.assertEquals(105, hypotheses.size()),
            () -> Assertions.assertEquals(70, cells.stream().distinct().count()),
            () -> Assertions.assertEquals(105, hypotheses.stream().distinct().count()),
            () -> Assertions.assertTrue(command.contains("-Dstep12.mixed.v2.seed=2026092701")),
            () -> Assertions.assertTrue(command.contains("-Dstep12.mixed.v2.iterations=10000")),
            () -> Assertions.assertTrue(command.contains("-Dstep12.mixed.v2.maxRounds=10000")),
            () -> Assertions.assertTrue(command.contains("-Dstep12.mixed.v2.workers=4")),
            () -> Assertions.assertTrue(command.contains("-Dstep12.mixed.v2.scenario=")),
            () -> Assertions.assertTrue(command.contains("-Dstep12.mixed.v2.statistics=")),
            () -> Assertions.assertTrue(command.endsWith(" clean test"))
        );
    }

    @Test
    void exactReportVerifierRequiresEverySuiteCleanlyAndRejectsExtras() throws Exception {
        final var reports = temporaryDirectory.resolve("surefire-reports");
        Files.createDirectories(reports);
        for (final var testClass : Step12MixedV2AcceptanceLedger.EXACT_TEST_CLASSES) {
            Files.writeString(
                reports.resolve("TEST-example." + testClass + ".xml"),
                "<testsuite tests=\"1\" errors=\"0\" failures=\"0\" skipped=\"0\"/>",
                StandardCharsets.UTF_8
            );
        }

        Assertions.assertDoesNotThrow(() ->
            Step12MixedV2VerificationOrchestrator.verifyExactReports(reports)
        );

        Files.writeString(
            reports.resolve("TEST-example.UnexpectedTest.xml"),
            "<testsuite tests=\"1\" errors=\"0\" failures=\"0\" skipped=\"0\"/>",
            StandardCharsets.UTF_8
        );
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> Step12MixedV2VerificationOrchestrator.verifyExactReports(reports)
        );

        Files.delete(reports.resolve("TEST-example.UnexpectedTest.xml"));
        final var skipped = Step12MixedV2AcceptanceLedger.EXACT_TEST_CLASSES.getFirst();
        Files.writeString(
            reports.resolve("TEST-example." + skipped + ".xml"),
            "<testsuite tests=\"1\" errors=\"0\" failures=\"0\" skipped=\"1\"/>",
            StandardCharsets.UTF_8
        );
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> Step12MixedV2VerificationOrchestrator.verifyExactReports(reports)
        );
    }

    private Step12MixedV2AcceptanceLedger ledger(AtomicReference<String> fingerprint) throws Exception {
        Files.createDirectories(temporaryDirectory.resolve("documentation"));
        Files.createDirectories(temporaryDirectory.resolve("game"));
        Files.writeString(temporaryDirectory.resolve("game/pom.xml"), "pom", StandardCharsets.UTF_8);
        return new Step12MixedV2AcceptanceLedger(temporaryDirectory, ignored -> fingerprint.get());
    }

    private static void verifyPrerequisites(Step12MixedV2AcceptanceLedger ledger) {
        ledger.runVerification(
            Step12MixedV2AcceptanceLedger.VerificationKind.EXACT_TESTS,
            Step12MixedV2AcceptanceLedgerTest::successfulCommand
        );
        ledger.runVerification(
            Step12MixedV2AcceptanceLedger.VerificationKind.FULL_BUILD,
            Step12MixedV2AcceptanceLedgerTest::successfulCommand
        );
    }

    private static int successfulCommand(List<String> command, Path workingDirectory, Path log)
        throws Exception {
        Files.writeString(
            log,
            String.join(" ", command) + "\n",
            StandardCharsets.UTF_8,
            StandardOpenOption.APPEND
        );
        return 0;
    }

    private static Step12MixedV2AcceptanceLedger.FinalAttempt beginFinal(
        Step12MixedV2AcceptanceLedger ledger
    ) {
        return ledger.beginFinal(
            Step12MixedV2AcceptanceLedger.FINAL_ROOT_SEED,
            Step12MixedV2AcceptanceLedger.FINAL_ITERATIONS,
            Step12MixedV2AcceptanceLedger.FINAL_MAX_ROUNDS,
            Step12MixedV2AcceptanceLedger.FINAL_WORKERS,
            Step12MixedV2AcceptanceLedger.expectedFinalCellDescriptors(),
            Step12MixedV2AcceptanceLedger.expectedFinalHypothesisDescriptors(),
            Step12MixedV2AcceptanceLedger.FINAL_REPORT_RELATIVE_PATH,
            Step12MixedV2AcceptanceLedger.FINAL_STATISTICS_RELATIVE_PATH
        );
    }

    private static Step12MixedV2AcceptanceLedger.FinalAttempt claimFinal(
        Step12MixedV2AcceptanceLedger ledger
    ) {
        return ledger.claimFinalExecution(
            Step12MixedV2AcceptanceLedger.FINAL_ROOT_SEED,
            Step12MixedV2AcceptanceLedger.FINAL_ITERATIONS,
            Step12MixedV2AcceptanceLedger.FINAL_MAX_ROUNDS,
            Step12MixedV2AcceptanceLedger.FINAL_WORKERS,
            Step12MixedV2AcceptanceLedger.expectedFinalCellDescriptors(),
            Step12MixedV2AcceptanceLedger.expectedFinalHypothesisDescriptors(),
            Step12MixedV2AcceptanceLedger.FINAL_REPORT_RELATIVE_PATH,
            Step12MixedV2AcceptanceLedger.FINAL_STATISTICS_RELATIVE_PATH
        );
    }

    private static void assertTamperDetectedAndRestore(
        Step12MixedV2AcceptanceLedger ledger,
        Path path
    ) throws Exception {
        final var original = Files.readAllBytes(path);
        Files.writeString(path, "tampered\n", StandardOpenOption.APPEND);
        Assertions.assertThrows(IllegalStateException.class, ledger::verifyFinalEvidence, path.toString());
        Files.write(path, original);
        Assertions.assertDoesNotThrow(ledger::verifyFinalEvidence, path.toString());
    }
}
