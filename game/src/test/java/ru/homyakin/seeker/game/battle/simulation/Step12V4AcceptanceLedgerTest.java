package ru.homyakin.seeker.game.battle.simulation;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.homyakin.seeker.game.battle.simulation.Step12V4AcceptanceLedger.RunKind;

class Step12V4AcceptanceLedgerTest {
    private static final List<String> EXPECTED_EXACT_TEST_CLASSES = List.of(
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
    private static final List<String> REQUIRED_INPUTS = List.of(
        ".gitignore",
        "documentation/raid-redesign-plan.md",
        "documentation/raid-redesign-step-12-battle-acceptance.md",
        "documentation/raid-redesign-step-12-v3-design.md",
        "documentation/raid-redesign-step-12-v4-design.md",
        "documentation/raid-redesign-step-6-skill-formulas.md",
        "documentation/raid-redesign-step-7-build-matrix.md",
        "documentation/raid-redesign-step-7-equipment-catalog.md",
        "documentation/raid-redesign-step-7-progression.md",
        "game/checkstyle-suppression.xml",
        "game/checkstyle.xml",
        "game/pom.xml",
        "game/run-step12-v4-verification.sh",
        "game/src/main/java/example/Main.java",
        "game/src/test/java/ru/homyakin/seeker/game/battle/Step12ExampleTest.java",
        "game/src/test/java/ru/homyakin/seeker/game/item/catalog/Step12CatalogExampleTest.java",
        "game/src/test/java/ru/homyakin/seeker/game/event/world_raid/entity/"
            + "WorldRaidPersonageSkillCompatibilityTest.java",
        "game/src/test/java/ru/homyakin/seeker/locale/battle/BattleLocalizationTest.java"
    );

    @TempDir
    private Path temporaryDirectory;

    @Test
    void resolvesRepositoryFromTestClassesAncestorsAndRequiredMarkers() throws Exception {
        final var checkout = createCheckout("root-resolution");
        final var testClasses = checkout.resolve("game/target/test-classes");
        Files.createDirectories(testClasses);

        Assertions.assertEquals(
            checkout.toRealPath(),
            Step12V4AcceptanceLedger.findRepositoryRoot(testClasses)
        );
    }

    @Test
    void burnsAtomicAttemptBeforeReportAndForbidsRetry() throws Exception {
        final var checkout = createCheckout("one-use");
        final var ledger = verifiedLedger(checkout);
        final var attempt = begin(ledger, RunKind.OUTCOME_PREFLIGHT, null);

        final var repeated = Assertions.assertThrows(
            IllegalStateException.class,
            () -> begin(ledger, RunKind.OUTCOME_PREFLIGHT, null)
        );

        Assertions.assertAll(
            () -> Assertions.assertTrue(Files.isRegularFile(attempt.marker())),
            () -> Assertions.assertFalse(Files.exists(attempt.report())),
            () -> Assertions.assertFalse(Files.exists(attempt.receipt())),
            () -> Assertions.assertTrue(repeated.getMessage().contains("cannot be repeated"))
        );
    }

    @Test
    void partialCellSetCannotBurnPreflightOrCreateSuccessArtifacts() throws Exception {
        final var checkout = createCheckout("partial-cells");
        final var ledger = verifiedLedger(checkout);
        final var kind = RunKind.OUTCOME_PREFLIGHT;

        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> ledger.begin(
                kind,
                kind.rootSeed(),
                kind.iterations(),
                kind.maxRounds(),
                List.of("CELL_A", "CELL_B"),
                null
            )
        );

        Assertions.assertAll(
            () -> Assertions.assertFalse(Files.exists(checkout.resolve(kind.markerRelativePath()))),
            () -> Assertions.assertFalse(Files.exists(checkout.resolve(kind.reportRelativePath()))),
            () -> Assertions.assertFalse(Files.exists(checkout.resolve(kind.receiptRelativePath())))
        );
    }

    @Test
    void invalidOutcomeWorkerCountIsRejectedBeforeLedgerMarker() throws Exception {
        final var checkout = createCheckout("invalid-workers");
        final var ledger = verifiedLedger(checkout);
        final var kind = RunKind.OUTCOME_PREFLIGHT;
        final var configuration = new Step12CombatMatrixSimulator.MatrixRunConfiguration(
            "V3",
            "PREFLIGHT",
            kind.iterations(),
            kind.maxRounds(),
            kind.rootSeed(),
            true,
            java.util.Set.of(),
            "V4"
        );

        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> Step12CombatMatrixSimulator.beginV4Acceptance(
                ledger,
                kind,
                configuration,
                kind.canonicalCellDescriptors(),
                null,
                0
            )
        );

        Assertions.assertFalse(Files.exists(checkout.resolve(kind.markerRelativePath())));
    }

    @Test
    void successfulCompletionWritesMachineReadableReceiptBoundToReport() throws Exception {
        final var checkout = createCheckout("success");
        final var ledger = verifiedLedger(checkout);
        final var attempt = begin(ledger, RunKind.OUTCOME_PREFLIGHT, null);
        ledger.writeReport(attempt, "# accepted\n");

        final var receipt = ledger.complete(attempt);
        final var receiptText = Files.readString(attempt.receipt(), StandardCharsets.UTF_8);

        Assertions.assertAll(
            () -> Assertions.assertEquals(RunKind.OUTCOME_PREFLIGHT, receipt.kind()),
            () -> Assertions.assertEquals(fileSha256(attempt.report()), receipt.reportSha256()),
            () -> Assertions.assertTrue(receiptText.contains("status=SUCCESS\n")),
            () -> Assertions.assertTrue(receiptText.contains("rootSeed=2026092203\n")),
            () -> Assertions.assertTrue(receiptText.contains("iterations=2000\n")),
            () -> Assertions.assertTrue(receiptText.contains("maxRounds=10000\n")),
            () -> Assertions.assertTrue(receiptText.contains(
                "report=documentation/raid-redesign-step-12-v4-outcome-preflight.md\n"
            )),
            () -> Assertions.assertTrue(receiptText.contains("reportSha256=" + receipt.reportSha256()))
        );
    }

    @Test
    void failedOrInterruptedAttemptKeepsMarkerAndCannotProduceReceipt() throws Exception {
        final var checkout = createCheckout("failed");
        final var ledger = verifiedLedger(checkout);
        complete(ledger, RunKind.OUTCOME_PREFLIGHT);
        final var attempt = begin(ledger, RunKind.CAUSAL_PREFLIGHT, null);

        Assertions.assertThrows(IllegalStateException.class, () -> ledger.complete(attempt));

        Assertions.assertAll(
            () -> Assertions.assertTrue(Files.isRegularFile(attempt.marker())),
            () -> Assertions.assertFalse(Files.exists(attempt.receipt())),
            () -> Assertions.assertThrows(
                IllegalStateException.class,
                () -> begin(ledger, RunKind.CAUSAL_PREFLIGHT, null)
            )
        );
    }

    @Test
    void causalPreflightRequiresSuccessfulOutcomePreflightBeforeBurningMarker() throws Exception {
        final var checkout = createCheckout("causal-preflight-order");
        final var ledger = verifiedLedger(checkout);

        Assertions.assertThrows(
            IllegalStateException.class,
            () -> begin(ledger, RunKind.CAUSAL_PREFLIGHT, null)
        );

        Assertions.assertFalse(Files.exists(
            checkout.resolve(RunKind.CAUSAL_PREFLIGHT.markerRelativePath())
        ));
    }

    @Test
    void fullBuildRequiresBothSuccessfulPreflightsBeforeBurningMarker() throws Exception {
        final var checkout = createCheckout("full-build-order");
        final var ledger = verifiedLedger(checkout);
        complete(ledger, RunKind.OUTCOME_PREFLIGHT);
        final var kind = Step12V4AcceptanceLedger.VerificationKind.FULL_BUILD;
        final var fingerprint = inputFingerprint(checkout);

        Assertions.assertThrows(
            IllegalStateException.class,
            () -> ledger.runVerification(kind, Step12V4AcceptanceLedgerTest::successfulCommand)
        );

        Assertions.assertAll(
            () -> Assertions.assertFalse(Files.exists(checkout.resolve(
                kind.markerRelativePath(fingerprint)
            ))),
            () -> Assertions.assertFalse(Files.exists(checkout.resolve(
                kind.logRelativePath(fingerprint)
            ))),
            () -> Assertions.assertFalse(Files.exists(checkout.resolve(
                kind.receiptRelativePath(fingerprint)
            )))
        );
    }

    @Test
    void causalFinalRequiresSuccessfulOutcomeFinalBeforeBurningMarker() throws Exception {
        final var checkout = createCheckout("causal-final-order");
        final var ledger = verifiedLedger(checkout);
        complete(ledger, RunKind.OUTCOME_PREFLIGHT);
        complete(ledger, RunKind.CAUSAL_PREFLIGHT);
        verifyFullBuild(ledger);

        Assertions.assertThrows(
            IllegalStateException.class,
            () -> begin(ledger, RunKind.CAUSAL_FINAL, null)
        );

        Assertions.assertFalse(Files.exists(
            checkout.resolve(RunKind.CAUSAL_FINAL.markerRelativePath())
        ));
    }

    @Test
    void completionRejectsCheckoutChangesMadeAfterAttemptStarted() throws Exception {
        final var checkout = createCheckout("changed-after-start");
        final var ledger = verifiedLedger(checkout);
        final var attempt = begin(ledger, RunKind.OUTCOME_PREFLIGHT, null);
        ledger.writeReport(attempt, "# generated from earlier inputs\n");
        Files.writeString(
            checkout.resolve("game/src/main/java/example/Main.java"),
            "changed",
            StandardCharsets.UTF_8
        );

        Assertions.assertThrows(IllegalArgumentException.class, () -> ledger.complete(attempt));
        Assertions.assertTrue(Files.isRegularFile(attempt.marker()));
        Assertions.assertFalse(Files.exists(attempt.receipt()));
    }

    @Test
    void finalRequiresBothUntamperedPreflightReceiptsFromTheSameSnapshot() throws Exception {
        final var checkout = createCheckout("final-ready");
        final var ledger = verifiedLedger(checkout);
        complete(ledger, RunKind.OUTCOME_PREFLIGHT);
        complete(ledger, RunKind.CAUSAL_PREFLIGHT);
        verifyFullBuild(ledger);

        final var finalAttempt = begin(ledger, RunKind.OUTCOME_FINAL, null);

        Assertions.assertAll(
            () -> Assertions.assertTrue(Files.isRegularFile(finalAttempt.marker())),
            () -> Assertions.assertEquals(2_026_092_302L, finalAttempt.rootSeed()),
            () -> Assertions.assertEquals(10_000, finalAttempt.iterations())
        );
    }

    @Test
    void finalRefusesTamperedPreflightReportWithoutBurningFinalMarker() throws Exception {
        final var checkout = createCheckout("tampered-preflight");
        final var ledger = verifiedLedger(checkout);
        final var outcome = complete(ledger, RunKind.OUTCOME_PREFLIGHT);
        complete(ledger, RunKind.CAUSAL_PREFLIGHT);
        verifyFullBuild(ledger);
        Files.writeString(outcome.report(), "tampered", StandardCharsets.UTF_8);

        Assertions.assertThrows(
            IllegalStateException.class,
            () -> begin(ledger, RunKind.OUTCOME_FINAL, null)
        );
        Assertions.assertFalse(Files.exists(checkout.resolve(RunKind.OUTCOME_FINAL.markerRelativePath())));
    }

    @Test
    void finalRefusesForgedPartialPreflightReceiptWithoutBurningFinalMarker() throws Exception {
        final var checkout = createCheckout("partial-preflight-receipt");
        final var ledger = verifiedLedger(checkout);
        final var outcome = complete(ledger, RunKind.OUTCOME_PREFLIGHT);
        complete(ledger, RunKind.CAUSAL_PREFLIGHT);
        verifyFullBuild(ledger);
        final var partialFingerprint = Step12V4AcceptanceLedger.cellSetFingerprint(
            List.of("CELL_A", "CELL_B")
        );
        replace(
            outcome.marker(),
            RunKind.OUTCOME_PREFLIGHT.expectedCellSetFingerprint(),
            partialFingerprint
        );
        replace(
            outcome.receipt(),
            RunKind.OUTCOME_PREFLIGHT.expectedCellSetFingerprint(),
            partialFingerprint
        );

        Assertions.assertThrows(
            IllegalStateException.class,
            () -> begin(ledger, RunKind.OUTCOME_FINAL, null)
        );
        Assertions.assertFalse(Files.exists(checkout.resolve(RunKind.OUTCOME_FINAL.markerRelativePath())));
    }

    @Test
    void completeFirstLayerRequiresBothUntamperedFinalReceipts() throws Exception {
        final var checkout = createCheckout("complete-final-evidence");
        final var ledger = verifiedLedger(checkout);
        complete(ledger, RunKind.OUTCOME_PREFLIGHT);
        complete(ledger, RunKind.CAUSAL_PREFLIGHT);
        verifyFullBuild(ledger);
        complete(ledger, RunKind.OUTCOME_FINAL);

        Assertions.assertThrows(IllegalStateException.class, ledger::verifyFinalEvidence);

        complete(ledger, RunKind.CAUSAL_FINAL);
        final var evidence = ledger.verifyFinalEvidence();

        Assertions.assertAll(
            () -> Assertions.assertEquals(RunKind.OUTCOME_FINAL, evidence.outcome().kind()),
            () -> Assertions.assertEquals(RunKind.CAUSAL_FINAL, evidence.causal().kind()),
            () -> Assertions.assertEquals(
                evidence.outcome().inputFingerprint(),
                evidence.causal().inputFingerprint()
            ),
            () -> Assertions.assertEquals(
                evidence.inputFingerprint(),
                evidence.outcome().inputFingerprint()
            )
        );

        Files.writeString(
            checkout.resolve(RunKind.CAUSAL_PREFLIGHT.reportRelativePath()),
            "tampered",
            StandardCharsets.UTF_8,
            StandardOpenOption.APPEND
        );
        Assertions.assertThrows(IllegalStateException.class, ledger::verifyFinalEvidence);
    }

    @Test
    void acceptanceOutputIsFixedButCellFingerprintIgnoresInputOrder() throws Exception {
        final var checkout = createCheckout("fixed-output");
        final var ledger = verifiedLedger(checkout);
        final var arbitraryOutput = "target/arbitrary.md";

        Assertions.assertAll(
            () -> Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> begin(ledger, RunKind.OUTCOME_PREFLIGHT, arbitraryOutput)
            ),
            () -> Assertions.assertEquals(
                Step12V4AcceptanceLedger.cellSetFingerprint(List.of("b", "a")),
                Step12V4AcceptanceLedger.cellSetFingerprint(List.of("a", "b"))
            ),
            () -> Assertions.assertNotEquals(
                Step12V4AcceptanceLedger.cellSetFingerprint(List.of("a", "b")),
                Step12V4AcceptanceLedger.cellSetFingerprint(List.of("a", "c"))
            ),
            () -> Assertions.assertFalse(Files.exists(
                checkout.resolve(RunKind.OUTCOME_PREFLIGHT.markerRelativePath())
            ))
        );
    }

    @Test
    void statisticalRunRequiresSuccessfulExactVerificationBeforeBurningItsMarker() throws Exception {
        final var checkout = createCheckout("missing-exact-verification");
        final var ledger = new Step12V4AcceptanceLedger(checkout);

        Assertions.assertThrows(
            IllegalStateException.class,
            () -> begin(ledger, RunKind.OUTCOME_PREFLIGHT, null)
        );
        Assertions.assertFalse(Files.exists(
            checkout.resolve(RunKind.OUTCOME_PREFLIGHT.markerRelativePath())
        ));
    }

    @Test
    void failedVerificationLeavesImmutableAttemptWithoutSuccessReceipt() throws Exception {
        final var checkout = createCheckout("failed-verification");
        final var ledger = new Step12V4AcceptanceLedger(checkout);
        final var kind = Step12V4AcceptanceLedger.VerificationKind.EXACT_TESTS;
        final var inputFingerprint = inputFingerprint(checkout);

        Assertions.assertThrows(
            IllegalStateException.class,
            () -> ledger.runVerification(kind, (command, workingDirectory, log) -> {
                Files.writeString(log, "failed\n", StandardCharsets.UTF_8, StandardOpenOption.APPEND);
                return 1;
            })
        );

        Assertions.assertAll(
            () -> Assertions.assertTrue(Files.isRegularFile(
                checkout.resolve(kind.markerRelativePath(inputFingerprint))
            )),
            () -> Assertions.assertTrue(Files.isRegularFile(
                checkout.resolve(kind.logRelativePath(inputFingerprint))
            )),
            () -> Assertions.assertFalse(Files.exists(
                checkout.resolve(kind.receiptRelativePath(inputFingerprint))
            )),
            () -> Assertions.assertThrows(
                IllegalStateException.class,
                () -> ledger.runVerification(kind, Step12V4AcceptanceLedgerTest::successfulCommand)
            )
        );
    }

    @Test
    void failedExactVerificationBurnsTheWholeV4RevisionAcrossInputFingerprints() throws Exception {
        final var checkout = createCheckout("failed-exact-new-fingerprint");
        final var ledger = new Step12V4AcceptanceLedger(checkout);
        final var kind = Step12V4AcceptanceLedger.VerificationKind.EXACT_TESTS;
        final var firstFingerprint = inputFingerprint(checkout);

        Assertions.assertThrows(
            IllegalStateException.class,
            () -> ledger.runVerification(kind, (command, workingDirectory, log) -> 1)
        );
        final var secondFingerprint = changeInputSnapshot(checkout);

        Assertions.assertThrows(
            IllegalStateException.class,
            () -> ledger.runVerification(kind, Step12V4AcceptanceLedgerTest::successfulCommand)
        );
        Assertions.assertAll(
            () -> Assertions.assertNotEquals(firstFingerprint, secondFingerprint),
            () -> Assertions.assertTrue(Files.isRegularFile(
                checkout.resolve(kind.markerRelativePath(firstFingerprint))
            )),
            () -> Assertions.assertFalse(Files.exists(
                checkout.resolve(kind.logRelativePath(secondFingerprint))
            )),
            () -> Assertions.assertFalse(Files.exists(
                checkout.resolve(kind.receiptRelativePath(secondFingerprint))
            ))
        );
    }

    @Test
    void successfulExactVerificationCannotBeRepeatedOnAnotherInputFingerprint() throws Exception {
        final var checkout = createCheckout("successful-exact-new-fingerprint");
        final var ledger = new Step12V4AcceptanceLedger(checkout);
        final var kind = Step12V4AcceptanceLedger.VerificationKind.EXACT_TESTS;
        final var first = ledger.runVerification(kind, Step12V4AcceptanceLedgerTest::successfulCommand);
        final var secondFingerprint = changeInputSnapshot(checkout);

        Assertions.assertThrows(
            IllegalStateException.class,
            () -> ledger.runVerification(kind, Step12V4AcceptanceLedgerTest::successfulCommand)
        );
        Assertions.assertAll(
            () -> Assertions.assertNotEquals(first.inputFingerprint(), secondFingerprint),
            () -> Assertions.assertTrue(Files.isRegularFile(
                checkout.resolve(kind.markerRelativePath(first.inputFingerprint()))
            )),
            () -> Assertions.assertFalse(Files.exists(
                checkout.resolve(kind.logRelativePath(secondFingerprint))
            )),
            () -> Assertions.assertFalse(Files.exists(
                checkout.resolve(kind.receiptRelativePath(secondFingerprint))
            ))
        );
    }

    @Test
    void anyFingerprintNamedExactAttemptMarkerAlsoBurnsTheV4Revision() throws Exception {
        final var checkout = createCheckout("preexisting-fingerprint-marker");
        final var ledger = new Step12V4AcceptanceLedger(checkout);
        final var kind = Step12V4AcceptanceLedger.VerificationKind.EXACT_TESTS;
        final var fingerprint = inputFingerprint(checkout);
        final var oldStyleMarker = checkout.resolve(
            "documentation/raid-redesign-step-12-v4-exact-tests-"
                + "b".repeat(64)
                + ".attempt.properties"
        );
        Files.writeString(oldStyleMarker, "existing attempt\n", StandardCharsets.UTF_8);

        Assertions.assertThrows(
            IllegalStateException.class,
            () -> ledger.runVerification(kind, Step12V4AcceptanceLedgerTest::successfulCommand)
        );
        Assertions.assertAll(
            () -> Assertions.assertFalse(Files.exists(
                checkout.resolve(kind.markerRelativePath(fingerprint))
            )),
            () -> Assertions.assertFalse(Files.exists(
                checkout.resolve(kind.logRelativePath(fingerprint))
            )),
            () -> Assertions.assertFalse(Files.exists(
                checkout.resolve(kind.receiptRelativePath(fingerprint))
            ))
        );
    }

    @Test
    void finalRunRequiresSuccessfulFullBuildReceiptBeforeBurningItsMarker() throws Exception {
        final var checkout = createCheckout("missing-full-build");
        final var ledger = verifiedLedger(checkout);
        complete(ledger, RunKind.OUTCOME_PREFLIGHT);
        complete(ledger, RunKind.CAUSAL_PREFLIGHT);

        Assertions.assertThrows(
            IllegalStateException.class,
            () -> begin(ledger, RunKind.OUTCOME_FINAL, null)
        );
        Assertions.assertFalse(Files.exists(checkout.resolve(RunKind.OUTCOME_FINAL.markerRelativePath())));
    }

    @Test
    void tamperedExactVerificationLogBlocksStatistics() throws Exception {
        final var checkout = createCheckout("tampered-exact-log");
        final var ledger = verifiedLedger(checkout);
        final var kind = Step12V4AcceptanceLedger.VerificationKind.EXACT_TESTS;
        final var inputFingerprint = inputFingerprint(checkout);
        Files.writeString(
            checkout.resolve(kind.logRelativePath(inputFingerprint)),
            "tampered",
            StandardCharsets.UTF_8,
            StandardOpenOption.APPEND
        );

        Assertions.assertThrows(
            IllegalStateException.class,
            () -> begin(ledger, RunKind.OUTCOME_PREFLIGHT, null)
        );
        Assertions.assertFalse(Files.exists(
            checkout.resolve(RunKind.OUTCOME_PREFLIGHT.markerRelativePath())
        ));
    }

    @Test
    void v4IdentitiesUseOnlyNewRootsAndArtifactPaths() {
        final var fingerprint = "a".repeat(64);
        Assertions.assertAll(
            () -> Assertions.assertEquals(2_026_092_203L, RunKind.OUTCOME_PREFLIGHT.rootSeed()),
            () -> Assertions.assertEquals(2_026_092_204L, RunKind.CAUSAL_PREFLIGHT.rootSeed()),
            () -> Assertions.assertEquals(2_026_092_302L, RunKind.OUTCOME_FINAL.rootSeed()),
            () -> Assertions.assertEquals(2_026_092_302L, RunKind.CAUSAL_FINAL.rootSeed()),
            () -> Assertions.assertEquals(10_000, RunKind.CAUSAL_FINAL.iterations()),
            () -> Assertions.assertEquals(10_000, RunKind.CAUSAL_FINAL.maxRounds()),
            () -> Assertions.assertEquals(
                "documentation/raid-redesign-step-12-v4-inputs.sha256",
                Step12V4AcceptanceLedger.MANIFEST_RELATIVE_PATH
            ),
            () -> Assertions.assertEquals(
                "documentation/raid-redesign-step-12-v4-exact-tests.attempt.properties",
                Step12V4AcceptanceLedger.VerificationKind.EXACT_TESTS.markerRelativePath(fingerprint)
            ),
            () -> Assertions.assertEquals(
                "documentation/raid-redesign-step-12-v4-causal-final.md",
                RunKind.CAUSAL_FINAL.reportRelativePath()
            ),
            () -> Assertions.assertTrue(java.util.Arrays.stream(RunKind.values())
                .map(RunKind::reportRelativePath)
                .allMatch(path -> path.contains("-v4-"))),
            () -> Assertions.assertTrue(java.util.Arrays.stream(
                Step12V4AcceptanceLedger.VerificationKind.values()
            )
                .map(kind -> kind.receiptRelativePath(fingerprint))
                .allMatch(path -> path.contains("-v4-")))
        );
    }

    @Test
    void exactVerificationCommandUsesTheFixedCompleteTestList() {
        final var sorted = Step12V4AcceptanceLedger.EXACT_TEST_CLASSES.stream().sorted().toList();
        final var command = Step12V4AcceptanceLedger.VerificationKind.EXACT_TESTS.command();

        Assertions.assertAll(
            () -> Assertions.assertEquals(
                EXPECTED_EXACT_TEST_CLASSES,
                Step12V4AcceptanceLedger.EXACT_TEST_CLASSES
            ),
            () -> Assertions.assertEquals(sorted, Step12V4AcceptanceLedger.EXACT_TEST_CLASSES),
            () -> Assertions.assertEquals(
                Step12V4AcceptanceLedger.EXACT_TEST_CLASSES.size(),
                Step12V4AcceptanceLedger.EXACT_TEST_CLASSES.stream().distinct().count()
            ),
            () -> Assertions.assertTrue(command.contains("Step12CatalogAcceptanceTest")),
            () -> Assertions.assertTrue(command.contains("ScalingSkillFormulaVersionBattleTest")),
            () -> Assertions.assertTrue(command.contains("InitGameDataTest")),
            () -> Assertions.assertTrue(command.contains("Step12V3CausalSimulatorTest")),
            () -> Assertions.assertTrue(command.endsWith(
                "-Dsurefire.failIfNoSpecifiedTests=true clean test"
            ))
        );
    }

    @Test
    void exactVerificationRequiresOneFreshSuccessfulReportPerFixedClass() throws Exception {
        final var reports = temporaryDirectory.resolve("surefire-reports");
        Files.createDirectories(reports);
        for (final var testClass : Step12V4AcceptanceLedger.EXACT_TEST_CLASSES) {
            Files.writeString(
                reports.resolve("TEST-example." + testClass + ".xml"),
                "<testsuite tests=\"1\" errors=\"0\" failures=\"0\" skipped=\"0\"/>",
                StandardCharsets.UTF_8
            );
        }

        Assertions.assertDoesNotThrow(() ->
            Step12V4VerificationOrchestrator.verifyExactReports(reports)
        );

        final var disabled = Step12V4AcceptanceLedger.EXACT_TEST_CLASSES.getFirst();
        Files.writeString(
            reports.resolve("TEST-example." + disabled + ".xml"),
            "<testsuite tests=\"1\" errors=\"0\" failures=\"0\" skipped=\"1\"/>",
            StandardCharsets.UTF_8
        );
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> Step12V4VerificationOrchestrator.verifyExactReports(reports)
        );
    }

    private Step12V4AcceptanceLedger.Attempt complete(
        Step12V4AcceptanceLedger ledger,
        RunKind kind
    ) {
        final var attempt = begin(ledger, kind, null);
        ledger.writeReport(attempt, "# " + kind + "\n");
        ledger.complete(attempt);
        return attempt;
    }

    private Step12V4AcceptanceLedger.Attempt begin(
        Step12V4AcceptanceLedger ledger,
        RunKind kind,
        String output
    ) {
        return ledger.begin(
            kind,
            kind.rootSeed(),
            kind.iterations(),
            kind.maxRounds(),
            kind.canonicalCellDescriptors(),
            output
        );
    }

    private static void replace(Path path, String expected, String replacement) throws Exception {
        final var original = Files.readString(path, StandardCharsets.UTF_8);
        Assertions.assertTrue(original.contains(expected));
        Files.writeString(
            path,
            original.replace(expected, replacement),
            StandardCharsets.UTF_8
        );
    }

    private Step12V4AcceptanceLedger verifiedLedger(Path checkout) {
        final var ledger = new Step12V4AcceptanceLedger(checkout);
        ledger.runVerification(
            Step12V4AcceptanceLedger.VerificationKind.EXACT_TESTS,
            Step12V4AcceptanceLedgerTest::successfulCommand
        );
        return ledger;
    }

    private void verifyFullBuild(Step12V4AcceptanceLedger ledger) {
        ledger.runVerification(
            Step12V4AcceptanceLedger.VerificationKind.FULL_BUILD,
            Step12V4AcceptanceLedgerTest::successfulCommand
        );
    }

    private static int successfulCommand(List<String> command, Path workingDirectory, Path log)
        throws Exception {
        Files.writeString(
            log,
            "command=" + String.join(" ", command) + "\nworkingDirectory=" + workingDirectory + "\n",
            StandardCharsets.UTF_8,
            StandardOpenOption.APPEND
        );
        return 0;
    }

    private static String inputFingerprint(Path checkout) {
        return Step12V4InputManifest.verifyAndFingerprint(
            checkout.resolve(Step12V4AcceptanceLedger.MANIFEST_RELATIVE_PATH),
            checkout
        );
    }

    private Path createCheckout(String name) throws Exception {
        final var checkout = temporaryDirectory.resolve(name);
        for (final var relative : REQUIRED_INPUTS) {
            final var input = checkout.resolve(relative);
            Files.createDirectories(input.getParent());
            Files.writeString(input, relative, StandardCharsets.UTF_8);
        }
        writeManifest(checkout);
        return checkout;
    }

    private String changeInputSnapshot(Path checkout) throws Exception {
        Files.writeString(
            checkout.resolve("game/src/main/java/example/Main.java"),
            "changed snapshot",
            StandardCharsets.UTF_8
        );
        writeManifest(checkout);
        return inputFingerprint(checkout);
    }

    private void writeManifest(Path checkout) throws Exception {
        final var inputs = new ArrayList<Path>();
        for (final var relative : REQUIRED_INPUTS) {
            inputs.add(checkout.resolve(relative));
        }
        final var lines = inputs.stream()
            .sorted(Comparator.comparing(path -> relative(checkout, path)))
            .map(path -> fileSha256(path) + "  " + relative(checkout, path))
            .toList();
        Files.writeString(
            checkout.resolve(Step12V4AcceptanceLedger.MANIFEST_RELATIVE_PATH),
            String.join("\n", lines) + "\n",
            StandardCharsets.UTF_8
        );
    }

    private static String relative(Path root, Path path) {
        return root.relativize(path).toString().replace('\\', '/');
    }

    private static String fileSha256(Path path) {
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path))
            );
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
