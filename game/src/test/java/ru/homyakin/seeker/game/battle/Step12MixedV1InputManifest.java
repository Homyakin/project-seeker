package ru.homyakin.seeker.game.battle;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/** Verifies the frozen input snapshot of the first mixed-build acceptance protocol. */
public final class Step12MixedV1InputManifest {
    static final String V4_MANIFEST_FINGERPRINT =
        "70d8b2be5ccbaa4c5e621c4a506f1f7d4ef9ada151daffd95b26357adc2a5a93";
    static final String V4_MAIN_TREE_SHA256 =
        "3ed3f4b06e918f04771d5e6560536b3c8d59fdc369491cf5e668e73ef96e0ed7";
    static final String V4_EVIDENCE_SET_SHA256 =
        "2b8c92c855f68c832fb335bfee7d54e047a581d2cd06c29f4b20b3274148ddc7";

    private static final Pattern ENTRY = Pattern.compile("([0-9a-f]{64})  (.+)");
    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");
    private static final String V4_MANIFEST = "documentation/raid-redesign-step-12-v4-inputs.sha256";
    private static final Set<String> REQUIRED_FILES = Set.of(
        ".gitignore",
        "documentation/raid-redesign-plan.md",
        "documentation/raid-redesign-step-12-battle-acceptance.md",
        "documentation/raid-redesign-step-12-mixed-compositions-design.md",
        "documentation/raid-redesign-step-6-skill-formulas.md",
        "documentation/raid-redesign-step-7-build-matrix.md",
        "documentation/raid-redesign-step-7-equipment-catalog.md",
        "documentation/raid-redesign-step-7-progression.md",
        "game/checkstyle-suppression.xml",
        "game/checkstyle.xml",
        "game/pom.xml",
        "game/run-step12-mixed-v1-verification.sh"
    );
    private static final List<String> REQUIRED_TREES = List.of(
        "game/src/main",
        "game/src/test"
    );
    private static final List<String> OPTIONAL_BUILD_TREES = List.of("game/.mvn");
    private static final List<String> V4_EVIDENCE_PATHS = List.of(
        "documentation/raid-redesign-step-12-v4-causal-final.attempt.properties",
        "documentation/raid-redesign-step-12-v4-causal-final.md",
        "documentation/raid-redesign-step-12-v4-causal-final.receipt.properties",
        "documentation/raid-redesign-step-12-v4-causal-preflight.attempt.properties",
        "documentation/raid-redesign-step-12-v4-causal-preflight.md",
        "documentation/raid-redesign-step-12-v4-causal-preflight.receipt.properties",
        "documentation/raid-redesign-step-12-v4-design.md",
        "documentation/raid-redesign-step-12-v4-exact-tests-"
            + V4_MANIFEST_FINGERPRINT + ".log",
        "documentation/raid-redesign-step-12-v4-exact-tests-"
            + V4_MANIFEST_FINGERPRINT + ".receipt.properties",
        "documentation/raid-redesign-step-12-v4-exact-tests.attempt.properties",
        "documentation/raid-redesign-step-12-v4-full-build-"
            + V4_MANIFEST_FINGERPRINT + ".attempt.properties",
        "documentation/raid-redesign-step-12-v4-full-build-"
            + V4_MANIFEST_FINGERPRINT + ".log",
        "documentation/raid-redesign-step-12-v4-full-build-"
            + V4_MANIFEST_FINGERPRINT + ".receipt.properties",
        V4_MANIFEST,
        "documentation/raid-redesign-step-12-v4-outcome-final.attempt.properties",
        "documentation/raid-redesign-step-12-v4-outcome-final.md",
        "documentation/raid-redesign-step-12-v4-outcome-final.receipt.properties",
        "documentation/raid-redesign-step-12-v4-outcome-preflight.attempt.properties",
        "documentation/raid-redesign-step-12-v4-outcome-preflight.md",
        "documentation/raid-redesign-step-12-v4-outcome-preflight.receipt.properties"
    );

    private Step12MixedV1InputManifest() {
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

    /** Verifies the exact mixed snapshot, the V4 evidence anchor and the unchanged gameplay tree. */
    public static String verifyAndFingerprint(Path manifest, Path inputRoot) {
        return verifyAndFingerprint(
            manifest,
            inputRoot,
            new V4Anchor(V4_MANIFEST_FINGERPRINT, V4_MAIN_TREE_SHA256, V4_EVIDENCE_SET_SHA256)
        );
    }

    static String verifyAndFingerprint(Path manifest, Path inputRoot, V4Anchor anchor) {
        final var root = canonicalDirectory(inputRoot);
        final var normalizedManifest = safeRegularFile(manifest, root, "input manifest");
        final var entries = readEntries(normalizedManifest);
        requireCanonicalEntries(entries, "Input manifest");
        verifyExactPathSet(root, entries);
        entries.forEach(entry -> verifyEntry(root, entry));
        verifyV4Anchor(root, anchor);
        return fingerprint(entries);
    }

    public static String fingerprint(Path manifest) {
        final var entries = readEntries(manifest.toAbsolutePath().normalize());
        requireCanonicalEntries(entries, "Input manifest");
        return fingerprint(entries);
    }

    static String fileSha256(Path path) {
        try {
            return sha256(Files.readAllBytes(path));
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot hash file: " + path, exception);
        }
    }

    static Set<String> requiredFixedPaths() {
        final var paths = new TreeSet<>(REQUIRED_FILES);
        paths.addAll(V4_EVIDENCE_PATHS);
        return Set.copyOf(paths);
    }

    static List<String> v4EvidencePaths() {
        return V4_EVIDENCE_PATHS;
    }

    private static void verifyV4Anchor(Path root, V4Anchor anchor) {
        if (!SHA_256.matcher(anchor.manifestFingerprint()).matches()
            || !SHA_256.matcher(anchor.mainTreeSha256()).matches()
            || !SHA_256.matcher(anchor.evidenceSetSha256()).matches()) {
            throw new IllegalArgumentException("V4 anchor must contain lowercase SHA-256 values");
        }
        final var v4ManifestPath = safeRegularFile(root.resolve(V4_MANIFEST), root, "V4 manifest");
        final var v4Entries = readEntries(v4ManifestPath);
        requireCanonicalEntries(v4Entries, "V4 manifest");
        final var v4Fingerprint = fingerprint(v4Entries);
        if (!v4Fingerprint.equals(anchor.manifestFingerprint())) {
            throw new IllegalArgumentException("V4 manifest fingerprint mismatch: " + v4Fingerprint);
        }

        final var evidenceLines = new ArrayList<String>(V4_EVIDENCE_PATHS.size());
        for (final var relativePath : V4_EVIDENCE_PATHS) {
            final var evidence = safeRegularFile(root.resolve(relativePath), root, "V4 evidence");
            evidenceLines.add(fileSha256(evidence) + "  " + relativePath);
        }
        evidenceLines.sort(Comparator.comparing(line -> line.substring(66)));
        final var evidenceFingerprint = sha256(String.join("\n", evidenceLines) + "\n");
        if (!evidenceFingerprint.equals(anchor.evidenceSetSha256())) {
            throw new IllegalArgumentException("V4 evidence-set fingerprint mismatch: " + evidenceFingerprint);
        }

        final var acceptedMain = new LinkedHashMap<String, String>();
        for (final var entry : v4Entries) {
            if (entry.relativePath().startsWith("game/src/main/")) {
                acceptedMain.put(entry.relativePath(), entry.sha256());
            }
        }
        final var acceptedMainFingerprint = sha256(acceptedMain.entrySet().stream()
            .map(entry -> entry.getValue() + "  " + entry.getKey())
            .collect(java.util.stream.Collectors.joining("\n", "", "\n")));
        if (!acceptedMainFingerprint.equals(anchor.mainTreeSha256())) {
            throw new IllegalArgumentException(
                "V4 gameplay-tree fingerprint mismatch: " + acceptedMainFingerprint
            );
        }
        final var currentMain = regularFiles(root, "game/src/main");
        if (!acceptedMain.keySet().equals(currentMain)) {
            final var missing = new TreeSet<>(acceptedMain.keySet());
            missing.removeAll(currentMain);
            final var extra = new TreeSet<>(currentMain);
            extra.removeAll(acceptedMain.keySet());
            throw new IllegalArgumentException(
                "Gameplay main tree differs from V4: missing=" + missing + "; extra=" + extra
            );
        }
        acceptedMain.forEach((relativePath, acceptedSha256) -> {
            final var input = safeRegularFile(root.resolve(relativePath), root, "V4 gameplay input");
            if (!fileSha256(input).equals(acceptedSha256)) {
                throw new IllegalArgumentException("Gameplay input differs from V4: " + relativePath);
            }
        });
    }

    private static void verifyExactPathSet(Path root, List<ManifestEntry> entries) {
        final var required = requiredPaths(root);
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

    private static Set<String> requiredPaths(Path root) {
        final var required = new TreeSet<String>();
        final var fixed = new TreeSet<>(REQUIRED_FILES);
        fixed.addAll(V4_EVIDENCE_PATHS);
        for (final var relativePath : fixed) {
            safeRegularFile(root.resolve(relativePath), root, "required manifest input");
            required.add(relativePath);
        }
        for (final var relativeTree : REQUIRED_TREES) {
            required.addAll(regularFiles(root, relativeTree));
        }
        for (final var relativeTree : OPTIONAL_BUILD_TREES) {
            if (Files.exists(root.resolve(relativeTree), LinkOption.NOFOLLOW_LINKS)) {
                required.addAll(regularFiles(root, relativeTree));
            }
        }
        return Set.copyOf(required);
    }

    private static Set<String> regularFiles(Path root, String relativeTree) {
        final var tree = root.resolve(relativeTree).normalize();
        if (!tree.startsWith(root) || !Files.isDirectory(tree) || Files.isSymbolicLink(tree)) {
            throw new IllegalArgumentException("Required manifest tree is missing or unsafe: " + tree);
        }
        final var result = new TreeSet<String>();
        try (var paths = Files.walk(tree)) {
            paths.forEach(path -> {
                if (Files.isSymbolicLink(path)) {
                    throw new IllegalArgumentException(
                        "Required manifest tree contains a symbolic link: " + path
                    );
                }
                if (Files.isRegularFile(path)) {
                    safeRegularFile(path, root, "manifest tree input");
                    result.add(relative(root, path));
                }
            });
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot enumerate required manifest tree: " + tree, exception);
        }
        return Set.copyOf(result);
    }

    private static List<ManifestEntry> readEntries(Path manifest) {
        final List<String> lines;
        try {
            lines = Files.readAllLines(manifest, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read input manifest: " + manifest, exception);
        }
        if (lines.isEmpty() || lines.stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException("Manifest must contain canonical non-blank entries");
        }
        return lines.stream().map(Step12MixedV1InputManifest::parseEntry).toList();
    }

    private static ManifestEntry parseEntry(String line) {
        final var matcher = ENTRY.matcher(line);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid input manifest entry: " + line);
        }
        return new ManifestEntry(matcher.group(1), matcher.group(2));
    }

    private static void requireCanonicalEntries(List<ManifestEntry> entries, String name) {
        if (entries.isEmpty()) {
            throw new IllegalArgumentException(name + " must contain at least one entry");
        }
        final var sorted = entries.stream()
            .sorted(Comparator.comparing(ManifestEntry::relativePath))
            .toList();
        if (!entries.equals(sorted)) {
            throw new IllegalArgumentException(name + " entries must be sorted by path");
        }
        if (entries.stream().map(ManifestEntry::relativePath).distinct().count() != entries.size()) {
            throw new IllegalArgumentException(name + " must not contain duplicate paths");
        }
    }

    private static void verifyEntry(Path root, ManifestEntry entry) {
        final var input = safeRegularFile(root.resolve(entry.relativePath()), root, "manifest input");
        if (!fileSha256(input).equals(entry.sha256())) {
            throw new IllegalArgumentException("Manifest checksum mismatch: " + entry.relativePath());
        }
    }

    private static String fingerprint(List<ManifestEntry> entries) {
        final var payload = entries.stream()
            .map(entry -> entry.sha256() + "  " + entry.relativePath())
            .collect(java.util.stream.Collectors.joining("\n", "", "\n"));
        return sha256(payload);
    }

    private static Path safeRegularFile(Path path, Path root, String name) {
        final var normalized = path.toAbsolutePath().normalize();
        if (!normalized.startsWith(root) || !Files.isRegularFile(normalized) || Files.isSymbolicLink(normalized)) {
            throw new IllegalArgumentException(name + " is outside the root, missing or symbolic: " + normalized);
        }
        try {
            if (!normalized.toRealPath().startsWith(root)) {
                throw new IllegalArgumentException(name + " resolves outside the root: " + normalized);
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("Cannot resolve " + name + ": " + normalized, exception);
        }
        return normalized;
    }

    private static Path canonicalDirectory(Path path) {
        try {
            final var canonical = path.toAbsolutePath().normalize().toRealPath();
            if (!Files.isDirectory(canonical)) {
                throw new IllegalArgumentException("Input root is not a directory: " + canonical);
            }
            return canonical;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Cannot resolve input root: " + path, exception);
        }
    }

    private static String relative(Path root, Path input) {
        return root.relativize(input.toAbsolutePath().normalize()).toString().replace('\\', '/');
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

    record V4Anchor(String manifestFingerprint, String mainTreeSha256, String evidenceSetSha256) {
    }

    private record ManifestEntry(String sha256, String relativePath) {
    }
}
