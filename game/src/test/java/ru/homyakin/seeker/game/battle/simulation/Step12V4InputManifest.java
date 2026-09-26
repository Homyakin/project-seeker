package ru.homyakin.seeker.game.battle.simulation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/** Verifies the immutable input snapshot required by step 12 acceptance runs. */
public final class Step12V4InputManifest {
    private static final Pattern ENTRY = Pattern.compile("([0-9a-f]{64})  (.+)");
    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");
    private static final Set<String> REQUIRED_FILES = Set.of(
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
        "game/run-step12-v4-verification.sh"
    );
    private static final List<String> REQUIRED_TREES = List.of(
        "game/src/main",
        "game/src/test"
    );

    private Step12V4InputManifest() {
    }

    public static void verify(Path manifest, Path inputRoot, String expectedFingerprint) {
        if (!SHA_256.matcher(expectedFingerprint).matches()) {
            throw new IllegalArgumentException("Input fingerprint must be a lowercase SHA-256 value");
        }
        final var actualFingerprint = verifyAndFingerprint(manifest, inputRoot);
        if (!actualFingerprint.equals(expectedFingerprint)) {
            throw new IllegalArgumentException(
                "Input fingerprint does not match manifest: " + actualFingerprint
            );
        }
    }

    /** Verifies the exact manifest snapshot and returns its computed fingerprint. */
    public static String verifyAndFingerprint(Path manifest, Path inputRoot) {
        final var normalizedManifest = manifest.toAbsolutePath().normalize();
        final var normalizedRoot = inputRoot.toAbsolutePath().normalize();
        if (!Files.isRegularFile(normalizedManifest)) {
            throw new IllegalArgumentException("Input manifest is not a regular file: " + normalizedManifest);
        }
        if (!Files.isDirectory(normalizedRoot)) {
            throw new IllegalArgumentException("Input root is not a directory: " + normalizedRoot);
        }

        final var entries = readEntries(normalizedManifest);
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("Input manifest must contain at least one entry");
        }
        final var sortedEntries = entries.stream()
            .sorted(Comparator.comparing(ManifestEntry::relativePath))
            .toList();
        if (!entries.equals(sortedEntries)) {
            throw new IllegalArgumentException("Input manifest entries must be sorted by path");
        }
        if (entries.stream().map(ManifestEntry::relativePath).distinct().count() != entries.size()) {
            throw new IllegalArgumentException("Input manifest must not contain duplicate paths");
        }
        verifyExactPathSet(normalizedRoot, entries);

        final var payload = entries.stream()
            .map(entry -> entry.sha256() + "  " + entry.relativePath())
            .collect(java.util.stream.Collectors.joining("\n", "", "\n"));
        final var actualFingerprint = sha256(payload.getBytes(StandardCharsets.UTF_8));
        entries.forEach(entry -> verifyEntry(normalizedRoot, entry));
        return actualFingerprint;
    }

    private static void verifyExactPathSet(Path inputRoot, List<ManifestEntry> entries) {
        final var required = requiredPaths(inputRoot);
        final var actual = entries.stream()
            .map(ManifestEntry::relativePath)
            .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
        final var missing = new TreeSet<>(required);
        missing.removeAll(actual);
        final var extra = new TreeSet<>(actual);
        extra.removeAll(required);
        if (!missing.isEmpty() || !extra.isEmpty()) {
            throw new IllegalArgumentException(
                "Input manifest path set mismatch: missing=" + missing + "; extra=" + extra
            );
        }
    }

    private static Set<String> requiredPaths(Path inputRoot) {
        final var required = new TreeSet<String>();
        for (final var relativePath : REQUIRED_FILES) {
            final var input = inputRoot.resolve(relativePath).normalize();
            if (!input.startsWith(inputRoot) || !Files.isRegularFile(input)) {
                throw new IllegalArgumentException("Required manifest input is missing: " + input);
            }
            required.add(relativePath);
        }
        for (final var relativeTree : REQUIRED_TREES) {
            final var tree = inputRoot.resolve(relativeTree).normalize();
            if (!tree.startsWith(inputRoot) || !Files.isDirectory(tree)) {
                throw new IllegalArgumentException("Required manifest tree is missing: " + tree);
            }
            try (var paths = Files.walk(tree)) {
                paths.filter(Files::isRegularFile)
                    .map(inputRoot::relativize)
                    .map(Path::toString)
                    .map(path -> path.replace('\\', '/'))
                    .forEach(required::add);
            } catch (IOException exception) {
                throw new IllegalStateException("Cannot enumerate required manifest tree: " + tree, exception);
            }
        }
        return Set.copyOf(required);
    }

    public static String fingerprint(Path manifest) {
        final var entries = readEntries(manifest.toAbsolutePath().normalize());
        final var payload = entries.stream()
            .map(entry -> entry.sha256() + "  " + entry.relativePath())
            .collect(java.util.stream.Collectors.joining("\n", "", "\n"));
        return sha256(payload.getBytes(StandardCharsets.UTF_8));
    }

    private static List<ManifestEntry> readEntries(Path manifest) {
        try {
            return Files.readAllLines(manifest, StandardCharsets.UTF_8).stream()
                .map(String::strip)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .map(Step12V4InputManifest::parseEntry)
                .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read input manifest: " + manifest, exception);
        }
    }

    private static ManifestEntry parseEntry(String line) {
        final var matcher = ENTRY.matcher(line);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid input manifest entry: " + line);
        }
        return new ManifestEntry(matcher.group(1), matcher.group(2));
    }

    private static void verifyEntry(Path inputRoot, ManifestEntry entry) {
        final var relative = Path.of(entry.relativePath());
        if (relative.isAbsolute()) {
            throw new IllegalArgumentException("Manifest path must be relative: " + relative);
        }
        final var input = inputRoot.resolve(relative).normalize();
        if (!input.startsWith(inputRoot) || !Files.isRegularFile(input)) {
            throw new IllegalArgumentException("Manifest input is outside the root or missing: " + input);
        }
        final String actual;
        try {
            actual = sha256(Files.readAllBytes(input));
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read manifest input: " + input, exception);
        }
        if (!actual.equals(entry.sha256())) {
            throw new IllegalArgumentException("Manifest checksum mismatch: " + entry.relativePath());
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private record ManifestEntry(String sha256, String relativePath) {
    }
}
