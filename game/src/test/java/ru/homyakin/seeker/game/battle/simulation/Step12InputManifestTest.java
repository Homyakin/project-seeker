package ru.homyakin.seeker.game.battle.simulation;

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

class Step12InputManifestTest {
    private static final List<String> FIXED_INPUTS = List.of(
        "documentation/raid-redesign-plan.md",
        "documentation/raid-redesign-step-12-battle-acceptance.md",
        "documentation/raid-redesign-step-12-v3-design.md",
        "documentation/raid-redesign-step-6-skill-formulas.md",
        "documentation/raid-redesign-step-7-build-matrix.md",
        "documentation/raid-redesign-step-7-equipment-catalog.md",
        "documentation/raid-redesign-step-7-progression.md",
        "game/checkstyle-suppression.xml",
        "game/checkstyle.xml",
        "game/pom.xml",
        "game/run-step12-v3-verification.sh",
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
    void verifiesFingerprintChecksumsAndTheExactRequiredPathSet() throws Exception {
        final var inputs = createRequiredInputs();
        final var manifest = writeManifest("inputs.sha256", inputs);
        final var fingerprint = Step12InputManifest.fingerprint(manifest);

        Assertions.assertAll(
            () -> Assertions.assertEquals(64, fingerprint.length()),
            () -> Assertions.assertDoesNotThrow(() ->
                Step12InputManifest.verify(manifest, temporaryDirectory, fingerprint)
            )
        );
    }

    @Test
    void rejectsChangedRequiredInput() throws Exception {
        final var inputs = createRequiredInputs();
        final var manifest = writeManifest("changed.sha256", inputs);
        final var fingerprint = Step12InputManifest.fingerprint(manifest);
        Files.writeString(
            temporaryDirectory.resolve("game/pom.xml"),
            "changed",
            StandardCharsets.UTF_8
        );

        final var exception = Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> Step12InputManifest.verify(manifest, temporaryDirectory, fingerprint)
        );

        Assertions.assertTrue(exception.getMessage().contains("checksum mismatch"));
    }

    @Test
    void rejectsMissingAndExtraManifestPaths() throws Exception {
        final var inputs = createRequiredInputs();
        final var missingPath = "game/src/main/java/example/Main.java";
        final var missing = writeManifest(
            "missing.sha256",
            inputs.stream().filter(path -> !relative(path).equals(missingPath)).toList()
        );
        final var extraInput = temporaryDirectory.resolve("extra.txt");
        Files.writeString(extraInput, "extra", StandardCharsets.UTF_8);
        final var withExtra = new ArrayList<>(inputs);
        withExtra.add(extraInput);
        final var extra = writeManifest("extra.sha256", withExtra);

        final var missingException = Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> Step12InputManifest.verify(
                missing,
                temporaryDirectory,
                Step12InputManifest.fingerprint(missing)
            )
        );
        final var extraException = Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> Step12InputManifest.verify(
                extra,
                temporaryDirectory,
                Step12InputManifest.fingerprint(extra)
            )
        );

        Assertions.assertAll(
            () -> Assertions.assertTrue(missingException.getMessage().contains(
                "missing=[" + missingPath + "]"
            )),
            () -> Assertions.assertTrue(extraException.getMessage().contains("extra=[extra.txt]"))
        );
    }

    @Test
    void rejectsUnsortedDuplicateEscapingAndInvalidFingerprint() throws Exception {
        final var inputs = createRequiredInputs();
        final var lines = manifestLines(inputs);
        final var valid = writeLines("valid.sha256", lines);
        final var unsortedLines = new ArrayList<>(lines);
        java.util.Collections.reverse(unsortedLines);
        final var unsorted = writeLines("unsorted.sha256", unsortedLines);
        final var duplicateLines = new ArrayList<>(lines);
        duplicateLines.add(lines.getLast());
        final var duplicate = writeLines("duplicate.sha256", duplicateLines);
        final var escapingLines = new ArrayList<>(lines);
        escapingLines.add(fileSha256(inputs.getFirst()) + "  ../outside.txt");
        escapingLines.sort(Comparator.comparing(Step12InputManifestTest::linePath));
        final var escaping = writeLines("escaping.sha256", escapingLines);

        Assertions.assertAll(
            () -> Assertions.assertThrows(IllegalArgumentException.class, () ->
                Step12InputManifest.verify(
                    unsorted,
                    temporaryDirectory,
                    Step12InputManifest.fingerprint(unsorted)
                )
            ),
            () -> Assertions.assertThrows(IllegalArgumentException.class, () ->
                Step12InputManifest.verify(
                    duplicate,
                    temporaryDirectory,
                    Step12InputManifest.fingerprint(duplicate)
                )
            ),
            () -> Assertions.assertThrows(IllegalArgumentException.class, () ->
                Step12InputManifest.verify(
                    escaping,
                    temporaryDirectory,
                    Step12InputManifest.fingerprint(escaping)
                )
            ),
            () -> Assertions.assertThrows(IllegalArgumentException.class, () ->
                Step12InputManifest.verify(valid, temporaryDirectory, "not-a-sha")
            )
        );
    }

    @Test
    void rejectsMissingRequiredFilesystemTreeBeforeReadingBattleInputs() throws Exception {
        final var inputs = createRequiredInputs();
        final var manifest = writeManifest("missing-tree.sha256", inputs);
        final var mainFile = temporaryDirectory.resolve("game/src/main/java/example/Main.java");
        Files.delete(mainFile);
        Files.delete(mainFile.getParent());
        Files.delete(mainFile.getParent().getParent());
        Files.delete(mainFile.getParent().getParent().getParent());

        final var exception = Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> Step12InputManifest.verify(
                manifest,
                temporaryDirectory,
                Step12InputManifest.fingerprint(manifest)
            )
        );

        Assertions.assertTrue(exception.getMessage().contains("Required manifest tree is missing"));
    }

    private List<Path> createRequiredInputs() throws Exception {
        final var inputs = new ArrayList<Path>();
        for (final var relative : FIXED_INPUTS) {
            final var input = temporaryDirectory.resolve(relative);
            Files.createDirectories(input.getParent());
            Files.writeString(input, relative, StandardCharsets.UTF_8);
            inputs.add(input);
        }
        return List.copyOf(inputs);
    }

    private Path writeManifest(String name, List<Path> inputs) throws Exception {
        return writeLines(name, manifestLines(inputs));
    }

    private Path writeLines(String name, List<String> lines) throws Exception {
        final var manifest = temporaryDirectory.resolve(name);
        Files.writeString(
            manifest,
            String.join("\n", lines) + "\n",
            StandardCharsets.UTF_8
        );
        return manifest;
    }

    private List<String> manifestLines(List<Path> inputs) throws Exception {
        final var lines = new ArrayList<String>();
        final var sortedInputs = inputs.stream()
            .sorted(Comparator.comparing(this::relative))
            .toList();
        for (final var input : sortedInputs) {
            lines.add(fileSha256(input) + "  " + relative(input));
        }
        return List.copyOf(lines);
    }

    private static String linePath(String line) {
        return line.substring(line.indexOf("  ") + 2);
    }

    private String relative(Path input) {
        return temporaryDirectory.relativize(input).toString().replace('\\', '/');
    }

    private static String fileSha256(Path file) throws Exception {
        return HexFormat.of().formatHex(
            MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file))
        );
    }
}
