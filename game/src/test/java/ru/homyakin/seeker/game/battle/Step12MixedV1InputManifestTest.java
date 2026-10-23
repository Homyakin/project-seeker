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

class Step12MixedV1InputManifestTest {
    @TempDir
    private Path temporaryDirectory;

    @Test
    void verifiesExactSnapshotV4EvidenceAndUnchangedMainTree() throws Exception {
        final var fixture = createFixture();

        final var fingerprint = Step12MixedV1InputManifest.verifyAndFingerprint(
            fixture.mixedManifest(),
            temporaryDirectory,
            fixture.anchor()
        );

        Assertions.assertAll(
            () -> Assertions.assertEquals(64, fingerprint.length()),
            () -> Assertions.assertEquals(
                fingerprint,
                Step12MixedV1InputManifest.fingerprint(fixture.mixedManifest())
            )
        );
    }

    @Test
    void rejectsGameplayChangeEvenWhenTheMixedManifestWasRecomputed() throws Exception {
        final var fixture = createFixture();
        Files.writeString(fixture.mainInput(), "changed gameplay", StandardCharsets.UTF_8);
        rewriteMixedManifest(fixture.mixedManifest());

        final var exception = Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> Step12MixedV1InputManifest.verifyAndFingerprint(
                fixture.mixedManifest(), temporaryDirectory, fixture.anchor()
            )
        );

        Assertions.assertTrue(exception.getMessage().contains("differs from V4"));
    }

    @Test
    void rejectsChangedV4EvidenceEvenWhenTheMixedManifestWasRecomputed() throws Exception {
        final var fixture = createFixture();
        final var evidence = temporaryDirectory.resolve("documentation/raid-redesign-step-12-v4-design.md");
        Files.writeString(evidence, "changed V4 evidence", StandardCharsets.UTF_8);
        rewriteMixedManifest(fixture.mixedManifest());

        final var exception = Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> Step12MixedV1InputManifest.verifyAndFingerprint(
                fixture.mixedManifest(), temporaryDirectory, fixture.anchor()
            )
        );

        Assertions.assertTrue(exception.getMessage().contains("evidence-set fingerprint mismatch"));
    }

    @Test
    void rejectsMissingExtraUnsortedAndSymbolicInputs() throws Exception {
        final var fixture = createFixture();
        final var lines = Files.readAllLines(fixture.mixedManifest(), StandardCharsets.UTF_8);
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
        extraLines.sort(Comparator.comparing(Step12MixedV1InputManifestTest::linePath));
        final var extra = temporaryDirectory.resolve("extra.sha256");
        Files.write(extra, extraLines, StandardCharsets.UTF_8);

        Assertions.assertAll(
            () -> Assertions.assertThrows(IllegalArgumentException.class, () ->
                Step12MixedV1InputManifest.verifyAndFingerprint(
                    missing, temporaryDirectory, fixture.anchor()
                )
            ),
            () -> Assertions.assertThrows(IllegalArgumentException.class, () ->
                Step12MixedV1InputManifest.verifyAndFingerprint(
                    unsorted, temporaryDirectory, fixture.anchor()
                )
            ),
            () -> Assertions.assertThrows(IllegalArgumentException.class, () ->
                Step12MixedV1InputManifest.verifyAndFingerprint(
                    extra, temporaryDirectory, fixture.anchor()
                )
            )
        );

        final var script = temporaryDirectory.resolve("game/run-step12-mixed-v1-verification.sh");
        final var realScript = temporaryDirectory.resolve("real-script.sh");
        Files.writeString(realScript, "script", StandardCharsets.UTF_8);
        Files.delete(script);
        Files.createSymbolicLink(script, realScript);
        rewriteMixedManifest(fixture.mixedManifest());
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> Step12MixedV1InputManifest.verifyAndFingerprint(
                fixture.mixedManifest(), temporaryDirectory, fixture.anchor()
            )
        );
    }

    @Test
    void rejectsSymbolicDirectoriesInsideRequiredTrees() throws Exception {
        final var fixture = createFixture();
        final var outside = temporaryDirectory.resolve("linked-tests");
        Files.createDirectories(outside);
        Files.writeString(outside.resolve("HiddenTest.java"), "hidden", StandardCharsets.UTF_8);
        Files.createSymbolicLink(
            temporaryDirectory.resolve("game/src/test/linked"),
            outside
        );

        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> Step12MixedV1InputManifest.verifyAndFingerprint(
                fixture.mixedManifest(), temporaryDirectory, fixture.anchor()
            )
        );
    }

    @Test
    void optionalMavenConfigurationBecomesARequiredFrozenInputWhenPresent() throws Exception {
        final var fixture = createFixture();
        final var mavenConfiguration = temporaryDirectory.resolve("game/.mvn/maven.config");
        Files.createDirectories(mavenConfiguration.getParent());
        Files.writeString(mavenConfiguration, "-T1", StandardCharsets.UTF_8);

        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> Step12MixedV1InputManifest.verifyAndFingerprint(
                fixture.mixedManifest(), temporaryDirectory, fixture.anchor()
            )
        );

        rewriteMixedManifest(fixture.mixedManifest());
        Assertions.assertDoesNotThrow(() -> Step12MixedV1InputManifest.verifyAndFingerprint(
            fixture.mixedManifest(), temporaryDirectory, fixture.anchor()
        ));

        Files.writeString(mavenConfiguration, "-T2", StandardCharsets.UTF_8);
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> Step12MixedV1InputManifest.verifyAndFingerprint(
                fixture.mixedManifest(), temporaryDirectory, fixture.anchor()
            )
        );
    }

    @Test
    void productionV4ConstantsMatchTheImmutableRepositoryArtifacts() throws Exception {
        final var root = Step12MixedV1AcceptanceLedger.findRepositoryRoot(
            Path.of(Step12MixedV1InputManifestTest.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI())
        );
        final var v4Manifest = root.resolve("documentation/raid-redesign-step-12-v4-inputs.sha256");
        final var evidenceLines = new ArrayList<String>();
        for (final var relativePath : Step12MixedV1InputManifest.v4EvidencePaths()) {
            evidenceLines.add(fileSha256(root.resolve(relativePath)) + "  " + relativePath);
        }
        evidenceLines.sort(Comparator.comparing(Step12MixedV1InputManifestTest::linePath));

        Assertions.assertAll(
            () -> Assertions.assertEquals(
                Step12MixedV1InputManifest.V4_MANIFEST_FINGERPRINT,
                fileSha256(v4Manifest)
            ),
            () -> Assertions.assertEquals(
                Step12MixedV1InputManifest.V4_EVIDENCE_SET_SHA256,
                sha256(String.join("\n", evidenceLines) + "\n")
            ),
            () -> Assertions.assertEquals(
                Step12MixedV1InputManifest.V4_MAIN_TREE_SHA256,
                sha256(Files.readAllLines(v4Manifest, StandardCharsets.UTF_8).stream()
                    .filter(line -> line.substring(66).startsWith("game/src/main/"))
                    .collect(java.util.stream.Collectors.joining("\n", "", "\n")))
            )
        );
    }

    private Fixture createFixture() throws Exception {
        for (final var relativePath : Step12MixedV1InputManifest.requiredFixedPaths()) {
            if (relativePath.equals("documentation/raid-redesign-step-12-v4-inputs.sha256")) {
                continue;
            }
            final var input = temporaryDirectory.resolve(relativePath);
            Files.createDirectories(input.getParent());
            Files.writeString(input, relativePath, StandardCharsets.UTF_8);
        }
        final var mainInput = temporaryDirectory.resolve("game/src/main/example/Main.java");
        final var testInput = temporaryDirectory.resolve("game/src/test/example/MainTest.java");
        Files.createDirectories(mainInput.getParent());
        Files.createDirectories(testInput.getParent());
        Files.writeString(mainInput, "gameplay", StandardCharsets.UTF_8);
        Files.writeString(testInput, "test", StandardCharsets.UTF_8);

        final var v4Manifest = temporaryDirectory.resolve(
            "documentation/raid-redesign-step-12-v4-inputs.sha256"
        );
        Files.writeString(
            v4Manifest,
            fileSha256(mainInput) + "  game/src/main/example/Main.java\n",
            StandardCharsets.UTF_8
        );
        final var anchor = new Step12MixedV1InputManifest.V4Anchor(
            fileSha256(v4Manifest),
            fileSha256(v4Manifest),
            v4EvidenceFingerprint()
        );
        final var mixedManifest = temporaryDirectory.resolve(
            "documentation/raid-redesign-step-12-mixed-v1-inputs.sha256"
        );
        rewriteMixedManifest(mixedManifest);
        return new Fixture(mixedManifest, mainInput, anchor);
    }

    private void rewriteMixedManifest(Path manifest) throws Exception {
        final var inputs = new ArrayList<Path>();
        for (final var relativePath : Step12MixedV1InputManifest.requiredFixedPaths()) {
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

    private String v4EvidenceFingerprint() {
        final var lines = Step12MixedV1InputManifest.v4EvidencePaths().stream()
            .map(relativePath -> fileSha256(temporaryDirectory.resolve(relativePath)) + "  " + relativePath)
            .sorted(Comparator.comparing(Step12MixedV1InputManifestTest::linePath))
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
        Path mixedManifest,
        Path mainInput,
        Step12MixedV1InputManifest.V4Anchor anchor
    ) {
    }
}
