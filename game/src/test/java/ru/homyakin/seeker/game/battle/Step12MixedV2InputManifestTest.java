package ru.homyakin.seeker.game.battle;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class Step12MixedV2InputManifestTest {
    @TempDir
    private Path temporaryDirectory;

    @Test
    void verifiesExactSnapshotRejectedEvidenceAndDeclaredDelta() throws Exception {
        final var fixture = createFixture();

        final var fingerprint = Step12MixedV2InputManifest.verifyAndFingerprint(
            fixture.currentManifest(),
            temporaryDirectory,
            fixture.anchor()
        );

        Assertions.assertAll(
            () -> Assertions.assertEquals(64, fingerprint.length()),
            () -> Assertions.assertEquals(
                fingerprint,
                Step12MixedV2InputManifest.fingerprint(fixture.currentManifest())
            )
        );
    }

    @Test
    void rejectsHistoricalInputChangeEvenWhenCurrentManifestWasRecomputed() throws Exception {
        final var fixture = createFixture();
        Files.writeString(fixture.unchangedMainInput(), "unexpected change", StandardCharsets.UTF_8);
        rewriteCurrentManifest(fixture.currentManifest());

        final var exception = Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> Step12MixedV2InputManifest.verifyAndFingerprint(
                fixture.currentManifest(), temporaryDirectory, fixture.anchor()
            )
        );

        Assertions.assertTrue(exception.getMessage().contains("Manifest checksum mismatch"));
    }

    @Test
    void rejectsMissingOrUnexpectedProductionCodeDelta() throws Exception {
        final var missingChange = createFixture();
        final var changedPath = Step12MixedV2InputManifest.changedMainPaths().stream()
            .sorted()
            .findFirst()
            .orElseThrow();
        Files.writeString(
            temporaryDirectory.resolve(changedPath),
            "historical " + changedPath,
            StandardCharsets.UTF_8
        );
        rewriteCurrentManifest(missingChange.currentManifest());
        final var missingChangeException = Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> Step12MixedV2InputManifest.verifyAndFingerprint(
                missingChange.currentManifest(), temporaryDirectory, missingChange.anchor()
            )
        );
        Assertions.assertTrue(missingChangeException.getMessage().contains("unexpected set"));

        clearFixture();
        final var extraFile = createFixture();
        final var unexpected = temporaryDirectory.resolve("game/src/main/unexpected.txt");
        Files.createDirectories(unexpected.getParent());
        Files.writeString(unexpected, "unexpected", StandardCharsets.UTF_8);
        rewriteCurrentManifest(extraFile.currentManifest());
        final var extraException = Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> Step12MixedV2InputManifest.verifyAndFingerprint(
                extraFile.currentManifest(), temporaryDirectory, extraFile.anchor()
            )
        );
        Assertions.assertTrue(extraException.getMessage().contains("unexpected path delta"));
    }

    @Test
    void rejectsChangedMixedV1EvidenceEvenWhenCurrentManifestWasRecomputed() throws Exception {
        final var fixture = createFixture();
        final var evidence = Step12MixedV2InputManifest.mixedV1EvidencePaths().stream()
            .filter(path -> !path.endsWith("inputs.sha256"))
            .sorted()
            .findFirst()
            .orElseThrow();
        Files.writeString(temporaryDirectory.resolve(evidence), "changed evidence", StandardCharsets.UTF_8);
        rewriteCurrentManifest(fixture.currentManifest());

        final var exception = Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> Step12MixedV2InputManifest.verifyAndFingerprint(
                fixture.currentManifest(), temporaryDirectory, fixture.anchor()
            )
        );

        Assertions.assertTrue(exception.getMessage().contains("evidence-set fingerprint mismatch"));
    }

    @Test
    void rejectsMissingDeclaredNonMainChange() throws Exception {
        final var fixture = createFixture();
        final var changedPath = Step12MixedV2InputManifest.changedNonMainPaths().stream()
            .sorted()
            .findFirst()
            .orElseThrow();
        Files.writeString(
            temporaryDirectory.resolve(changedPath),
            "historical " + changedPath,
            StandardCharsets.UTF_8
        );
        rewriteCurrentManifest(fixture.currentManifest());

        final var exception = Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> Step12MixedV2InputManifest.verifyAndFingerprint(
                fixture.currentManifest(), temporaryDirectory, fixture.anchor()
            )
        );

        Assertions.assertTrue(exception.getMessage().contains("declared non-main files"));
    }

    @Test
    void rejectsContradictoryMixedV1SuccessReceiptEvenOutsideManifest() throws Exception {
        final var fixture = createFixture();
        final var forbiddenReceipt = temporaryDirectory.resolve(
            Step12MixedV2InputManifest.mixedV1SuccessReceiptPath()
        );
        Files.writeString(forbiddenReceipt, "status=SUCCESS\n", StandardCharsets.UTF_8);

        final var exception = Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> Step12MixedV2InputManifest.verifyAndFingerprint(
                fixture.currentManifest(), temporaryDirectory, fixture.anchor()
            )
        );

        Assertions.assertTrue(exception.getMessage().contains("must be absent"));
    }

    @Test
    void rejectsMissingExtraUnsortedAndSymbolicInputs() throws Exception {
        final var fixture = createFixture();
        final var lines = Files.readAllLines(fixture.currentManifest(), StandardCharsets.UTF_8);
        final var missing = temporaryDirectory.resolve("missing.sha256");
        Files.write(missing, lines.subList(1, lines.size()), StandardCharsets.UTF_8);
        final var unsortedLines = new ArrayList<>(lines);
        java.util.Collections.reverse(unsortedLines);
        final var unsorted = temporaryDirectory.resolve("unsorted.sha256");
        Files.write(unsorted, unsortedLines, StandardCharsets.UTF_8);
        final var extraInput = temporaryDirectory.resolve("extra.txt");
        Files.writeString(extraInput, "extra", StandardCharsets.UTF_8);
        final var extraLines = new ArrayList<>(lines);
        extraLines.add(fileSha256(extraInput) + "  extra.txt");
        extraLines.sort(Comparator.comparing(Step12MixedV2InputManifestTest::linePath));
        final var extra = temporaryDirectory.resolve("extra.sha256");
        Files.write(extra, extraLines, StandardCharsets.UTF_8);

        Assertions.assertAll(
            () -> Assertions.assertThrows(IllegalArgumentException.class, () ->
                Step12MixedV2InputManifest.verifyAndFingerprint(
                    missing, temporaryDirectory, fixture.anchor()
                )
            ),
            () -> Assertions.assertThrows(IllegalArgumentException.class, () ->
                Step12MixedV2InputManifest.verifyAndFingerprint(
                    unsorted, temporaryDirectory, fixture.anchor()
                )
            ),
            () -> Assertions.assertThrows(IllegalArgumentException.class, () ->
                Step12MixedV2InputManifest.verifyAndFingerprint(
                    extra, temporaryDirectory, fixture.anchor()
                )
            )
        );

        final var script = temporaryDirectory.resolve("game/run-step12-mixed-v2-verification.sh");
        final var realScript = temporaryDirectory.resolve("real-script.sh");
        Files.writeString(realScript, "script", StandardCharsets.UTF_8);
        Files.delete(script);
        Files.createSymbolicLink(script, realScript);
        rewriteCurrentManifest(fixture.currentManifest());
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> Step12MixedV2InputManifest.verifyAndFingerprint(
                fixture.currentManifest(), temporaryDirectory, fixture.anchor()
            )
        );
    }

    @Test
    void optionalMavenConfigurationBecomesRequiredWhenPresent() throws Exception {
        final var fixture = createFixture();
        final var mavenConfiguration = temporaryDirectory.resolve("game/.mvn/maven.config");
        Files.createDirectories(mavenConfiguration.getParent());
        Files.writeString(mavenConfiguration, "-T1", StandardCharsets.UTF_8);

        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> Step12MixedV2InputManifest.verifyAndFingerprint(
                fixture.currentManifest(), temporaryDirectory, fixture.anchor()
            )
        );

        rewriteCurrentManifest(fixture.currentManifest());
        Assertions.assertDoesNotThrow(() -> Step12MixedV2InputManifest.verifyAndFingerprint(
            fixture.currentManifest(), temporaryDirectory, fixture.anchor()
        ));
    }

    @Test
    void productionMixedV1ConstantsMatchImmutableRepositoryArtifacts() throws Exception {
        final var root = Step12MixedV2AcceptanceLedger.findRepositoryRoot(
            Path.of(Step12MixedV2InputManifestTest.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI())
        );
        final var manifest = root.resolve(
            "documentation/raid-redesign-step-12-mixed-v1-inputs.sha256"
        );
        final var evidenceLines = new ArrayList<String>();
        for (final var relativePath : Step12MixedV2InputManifest.mixedV1EvidencePaths()) {
            evidenceLines.add(fileSha256(root.resolve(relativePath)) + "  " + relativePath);
        }
        evidenceLines.sort(Comparator.comparing(Step12MixedV2InputManifestTest::linePath));

        Assertions.assertAll(
            () -> Assertions.assertEquals(
                Step12MixedV2InputManifest.MIXED_V1_MANIFEST_FINGERPRINT,
                fileSha256(manifest)
            ),
            () -> Assertions.assertEquals(
                Step12MixedV2InputManifest.MIXED_V1_EVIDENCE_SET_SHA256,
                sha256(String.join("\n", evidenceLines) + "\n")
            ),
            () -> Assertions.assertEquals(
                Step12MixedV2InputManifest.MIXED_V1_MAIN_TREE_SHA256,
                sha256(Files.readAllLines(manifest, StandardCharsets.UTF_8).stream()
                    .filter(line -> line.substring(66).startsWith("game/src/main/"))
                    .collect(java.util.stream.Collectors.joining("\n", "", "\n")))
            )
        );
    }

    private Fixture createFixture() throws Exception {
        for (final var relativePath : Step12MixedV2InputManifest.requiredFixedPaths()) {
            if (relativePath.equals("documentation/raid-redesign-step-12-mixed-v1-inputs.sha256")) {
                continue;
            }
            final var input = temporaryDirectory.resolve(relativePath);
            Files.createDirectories(input.getParent());
            Files.writeString(input, relativePath, StandardCharsets.UTF_8);
        }

        final var historicalInputs = new ArrayList<Path>();
        final var unchangedMain = temporaryDirectory.resolve("game/src/main/example/Main.java");
        Files.createDirectories(unchangedMain.getParent());
        Files.writeString(unchangedMain, "historical unchanged", StandardCharsets.UTF_8);
        historicalInputs.add(unchangedMain);
        for (final var relativePath : Step12MixedV2InputManifest.changedMainPaths()) {
            final var input = temporaryDirectory.resolve(relativePath);
            Files.createDirectories(input.getParent());
            Files.writeString(input, "historical " + relativePath, StandardCharsets.UTF_8);
            historicalInputs.add(input);
        }
        for (final var relativePath : Step12MixedV2InputManifest.changedNonMainPaths()) {
            final var input = temporaryDirectory.resolve(relativePath);
            Files.createDirectories(input.getParent());
            Files.writeString(input, "historical " + relativePath, StandardCharsets.UTF_8);
            historicalInputs.add(input);
        }
        final var testInput = temporaryDirectory.resolve("game/src/test/example/MainTest.java");
        Files.createDirectories(testInput.getParent());
        Files.writeString(testInput, "historical test", StandardCharsets.UTF_8);
        historicalInputs.add(testInput);

        final var historicalManifest = temporaryDirectory.resolve(
            "documentation/raid-redesign-step-12-mixed-v1-inputs.sha256"
        );
        writeManifest(historicalManifest, historicalInputs);
        final var historicalMainFingerprint = sha256(
            Files.readAllLines(historicalManifest, StandardCharsets.UTF_8).stream()
                .filter(line -> line.substring(66).startsWith("game/src/main/"))
                .collect(java.util.stream.Collectors.joining("\n", "", "\n"))
        );

        for (final var relativePath : Step12MixedV2InputManifest.changedMainPaths()) {
            Files.writeString(
                temporaryDirectory.resolve(relativePath),
                "candidate " + relativePath,
                StandardCharsets.UTF_8
            );
        }
        for (final var relativePath : Step12MixedV2InputManifest.changedNonMainPaths()) {
            Files.writeString(
                temporaryDirectory.resolve(relativePath),
                "candidate " + relativePath,
                StandardCharsets.UTF_8
            );
        }
        for (final var relativePath : Step12MixedV2InputManifest.addedMainPaths()) {
            final var input = temporaryDirectory.resolve(relativePath);
            Files.createDirectories(input.getParent());
            Files.writeString(input, "added " + relativePath, StandardCharsets.UTF_8);
        }

        final var anchor = new Step12MixedV2InputManifest.MixedV1Anchor(
            fileSha256(historicalManifest),
            historicalMainFingerprint,
            mixedV1EvidenceFingerprint()
        );
        final var currentManifest = temporaryDirectory.resolve(
            "documentation/raid-redesign-step-12-mixed-v2-inputs.sha256"
        );
        rewriteCurrentManifest(currentManifest);
        return new Fixture(currentManifest, unchangedMain, anchor);
    }

    private void clearFixture() throws Exception {
        try (var paths = Files.walk(temporaryDirectory)) {
            paths.sorted(Comparator.reverseOrder())
                .filter(path -> !path.equals(temporaryDirectory))
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (Exception exception) {
                        throw new IllegalStateException(exception);
                    }
                });
        }
    }

    private void rewriteCurrentManifest(Path manifest) throws Exception {
        final var inputs = new ArrayList<Path>();
        for (final var relativePath : Step12MixedV2InputManifest.requiredFixedPaths()) {
            inputs.add(temporaryDirectory.resolve(relativePath));
        }
        try (var paths = Files.walk(temporaryDirectory.resolve("game/src/main"))) {
            paths.filter(Files::isRegularFile).forEach(inputs::add);
        }
        try (var paths = Files.walk(temporaryDirectory.resolve("game/src/test"))) {
            paths.filter(Files::isRegularFile).forEach(inputs::add);
        }
        final var mavenConfiguration = temporaryDirectory.resolve("game/.mvn");
        if (Files.isDirectory(mavenConfiguration)) {
            try (var paths = Files.walk(mavenConfiguration)) {
                paths.filter(Files::isRegularFile).forEach(inputs::add);
            }
        }
        writeManifest(manifest, inputs);
    }

    private void writeManifest(Path manifest, List<Path> inputs) throws Exception {
        final var lines = inputs.stream()
            .distinct()
            .sorted(Comparator.comparing(this::relative))
            .map(path -> fileSha256(path) + "  " + relative(path))
            .toList();
        Files.writeString(manifest, String.join("\n", lines) + "\n", StandardCharsets.UTF_8);
    }

    private String mixedV1EvidenceFingerprint() {
        final var lines = Step12MixedV2InputManifest.mixedV1EvidencePaths().stream()
            .map(relativePath -> fileSha256(temporaryDirectory.resolve(relativePath)) + "  " + relativePath)
            .sorted(Comparator.comparing(Step12MixedV2InputManifestTest::linePath))
            .toList();
        return sha256(String.join("\n", lines) + "\n");
    }

    private String relative(Path input) {
        return temporaryDirectory.relativize(input).toString().replace('\\', '/');
    }

    private static String linePath(String line) {
        return line.substring(line.indexOf("  ") + 2);
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

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private record Fixture(
        Path currentManifest,
        Path unchangedMainInput,
        Step12MixedV2InputManifest.MixedV1Anchor anchor
    ) {
    }
}
