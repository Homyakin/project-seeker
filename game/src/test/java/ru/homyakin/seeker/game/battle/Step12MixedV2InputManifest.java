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

/** Verifies the frozen inputs and the exact delta from the rejected mixed-build protocol. */
public final class Step12MixedV2InputManifest {
    static final String MIXED_V1_MANIFEST_FINGERPRINT =
        "f7f30d9e47210b765c802f8fde679629cf46c69e37aeb9bd0b9785a0d21a2d6e";
    static final String MIXED_V1_MAIN_TREE_SHA256 =
        "3ed3f4b06e918f04771d5e6560536b3c8d59fdc369491cf5e668e73ef96e0ed7";
    static final String MIXED_V1_EVIDENCE_SET_SHA256 =
        "1f4b47245eb1f3e7619251aaedb0a7928f0e931ef6f69924eb516ded46223a66";

    private static final Pattern ENTRY = Pattern.compile("([0-9a-f]{64})  (.+)");
    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");
    private static final String MIXED_V1_MANIFEST =
        "documentation/raid-redesign-step-12-mixed-v1-inputs.sha256";
    private static final String MIXED_V1_SUCCESS_RECEIPT =
        "documentation/raid-redesign-step-12-mixed-v1-final-2026092601.receipt.properties";
    private static final Set<String> REQUIRED_FILES = Set.of(
        ".gitignore",
        "documentation/raid-redesign-plan.md",
        "documentation/raid-redesign-step-12-battle-acceptance.md",
        "documentation/raid-redesign-step-12-mixed-compositions-design.md",
        "documentation/raid-redesign-step-12-mixed-v2-design.md",
        "documentation/raid-redesign-step-12-mixed-v2-diagnostic-statistics.tsv",
        "documentation/raid-redesign-step-12-mixed-v2-diagnostic.md",
        "documentation/raid-redesign-step-6-skill-formulas.md",
        "documentation/raid-redesign-step-7-bow-correction.md",
        "documentation/raid-redesign-step-7-build-matrix.md",
        "documentation/raid-redesign-step-7-equipment-catalog.md",
        "documentation/raid-redesign-step-7-progression.md",
        "game/checkstyle-suppression.xml",
        "game/checkstyle.xml",
        "game/pom.xml",
        "game/run-step12-mixed-v2-verification.sh"
    );
    private static final List<String> REQUIRED_TREES = List.of(
        "game/src/main",
        "game/src/test"
    );
    private static final List<String> OPTIONAL_BUILD_TREES = List.of("game/.mvn");
    private static final List<String> MIXED_V1_EVIDENCE_PATHS = List.of(
        "documentation/raid-redesign-step-12-mixed-compositions-design.md",
        "documentation/raid-redesign-step-12-mixed-final-seed-2026092601.attempt.properties",
        "documentation/raid-redesign-step-12-mixed-final-seed-2026092601.battle-attempt.properties",
        "documentation/raid-redesign-step-12-mixed-v1-exact-tests-"
            + MIXED_V1_MANIFEST_FINGERPRINT + ".log",
        "documentation/raid-redesign-step-12-mixed-v1-exact-tests-"
            + MIXED_V1_MANIFEST_FINGERPRINT + ".receipt.properties",
        "documentation/raid-redesign-step-12-mixed-v1-exact-tests.attempt.properties",
        "documentation/raid-redesign-step-12-mixed-v1-final-2026092601.log",
        "documentation/raid-redesign-step-12-mixed-v1-final-statistics.tsv",
        "documentation/raid-redesign-step-12-mixed-v1-final.md",
        "documentation/raid-redesign-step-12-mixed-v1-full-build-"
            + MIXED_V1_MANIFEST_FINGERPRINT + ".attempt.properties",
        "documentation/raid-redesign-step-12-mixed-v1-full-build-"
            + MIXED_V1_MANIFEST_FINGERPRINT + ".log",
        "documentation/raid-redesign-step-12-mixed-v1-full-build-"
            + MIXED_V1_MANIFEST_FINGERPRINT + ".receipt.properties",
        MIXED_V1_MANIFEST
    );
    private static final Set<String> CHANGED_MAIN_PATHS = Set.of(
        "game/src/main/java/ru/homyakin/seeker/game/item/catalog/EquipmentCatalogRelease.java",
        "game/src/main/java/ru/homyakin/seeker/game/item/catalog/EquipmentCatalogValidator.java",
        "game/src/main/java/ru/homyakin/seeker/game/item/catalog/EquipmentCatalogVersion.java"
    );
    private static final Set<String> CHANGED_NON_MAIN_PATHS = Set.of(
        ".gitignore",
        "documentation/raid-redesign-plan.md",
        "documentation/raid-redesign-step-12-battle-acceptance.md",
        "documentation/raid-redesign-step-7-build-matrix.md",
        "documentation/raid-redesign-step-7-equipment-catalog.md",
        "documentation/raid-redesign-step-7-progression.md",
        "game/src/test/java/ru/homyakin/seeker/game/item/catalog/EquipmentCatalogLoaderTest.java",
        "game/src/test/java/ru/homyakin/seeker/game/item/catalog/EquipmentCatalogValidatorTest.java"
    );
    private static final Set<String> ADDED_MAIN_PATHS = Set.of(
        "game/src/main/resources/game-data/catalog/scaling_v3/default_items.toml",
        "game/src/main/resources/game-data/catalog/scaling_v3/item_modifiers.toml",
        "game/src/main/resources/game-data/catalog/scaling_v3/item_objects.toml"
    );

    private Step12MixedV2InputManifest() {
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

    /** Verifies the new snapshot, the rejected V1 evidence and every declared delta. */
    public static String verifyAndFingerprint(Path manifest, Path inputRoot) {
        return verifyAndFingerprint(
            manifest,
            inputRoot,
            new MixedV1Anchor(
                MIXED_V1_MANIFEST_FINGERPRINT,
                MIXED_V1_MAIN_TREE_SHA256,
                MIXED_V1_EVIDENCE_SET_SHA256
            )
        );
    }

    static String verifyAndFingerprint(Path manifest, Path inputRoot, MixedV1Anchor anchor) {
        final var root = canonicalDirectory(inputRoot);
        final var normalizedManifest = safeRegularFile(manifest, root, "input manifest");
        final var entries = readEntries(normalizedManifest);
        requireCanonicalEntries(entries, "Input manifest");
        verifyExactPathSet(root, entries);
        entries.forEach(entry -> verifyEntry(root, entry));
        verifyMixedV1Anchor(root, anchor);
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
        paths.addAll(MIXED_V1_EVIDENCE_PATHS);
        return Set.copyOf(paths);
    }

    static List<String> mixedV1EvidencePaths() {
        return MIXED_V1_EVIDENCE_PATHS;
    }

    static Set<String> changedMainPaths() {
        return CHANGED_MAIN_PATHS;
    }

    static Set<String> changedNonMainPaths() {
        return CHANGED_NON_MAIN_PATHS;
    }

    static Set<String> addedMainPaths() {
        return ADDED_MAIN_PATHS;
    }

    static String mixedV1SuccessReceiptPath() {
        return MIXED_V1_SUCCESS_RECEIPT;
    }

    private static void verifyMixedV1Anchor(Path root, MixedV1Anchor anchor) {
        if (!SHA_256.matcher(anchor.manifestFingerprint()).matches()
            || !SHA_256.matcher(anchor.mainTreeSha256()).matches()
            || !SHA_256.matcher(anchor.evidenceSetSha256()).matches()) {
            throw new IllegalArgumentException("Mixed V1 anchor must contain lowercase SHA-256 values");
        }
        requireAbsentPath(root, MIXED_V1_SUCCESS_RECEIPT, "rejected mixed V1 success receipt");
        final var mixedV1ManifestPath = safeRegularFile(
            root.resolve(MIXED_V1_MANIFEST),
            root,
            "mixed V1 manifest"
        );
        final var mixedV1Entries = readEntries(mixedV1ManifestPath);
        requireCanonicalEntries(mixedV1Entries, "Mixed V1 manifest");
        final var mixedV1Fingerprint = fingerprint(mixedV1Entries);
        if (!mixedV1Fingerprint.equals(anchor.manifestFingerprint())) {
            throw new IllegalArgumentException(
                "Mixed V1 manifest fingerprint mismatch: " + mixedV1Fingerprint
            );
        }

        final var evidenceLines = new ArrayList<String>(MIXED_V1_EVIDENCE_PATHS.size());
        for (final var relativePath : MIXED_V1_EVIDENCE_PATHS) {
            final var evidence = safeRegularFile(root.resolve(relativePath), root, "mixed V1 evidence");
            evidenceLines.add(fileSha256(evidence) + "  " + relativePath);
        }
        evidenceLines.sort(Comparator.comparing(line -> line.substring(66)));
        final var evidenceFingerprint = sha256(String.join("\n", evidenceLines) + "\n");
        if (!evidenceFingerprint.equals(anchor.evidenceSetSha256())) {
            throw new IllegalArgumentException(
                "Mixed V1 evidence-set fingerprint mismatch: " + evidenceFingerprint
            );
        }

        final var acceptedMain = new LinkedHashMap<String, String>();
        for (final var entry : mixedV1Entries) {
            if (entry.relativePath().startsWith("game/src/main/")) {
                acceptedMain.put(entry.relativePath(), entry.sha256());
            }
        }
        final var acceptedMainFingerprint = sha256(acceptedMain.entrySet().stream()
            .map(entry -> entry.getValue() + "  " + entry.getKey())
            .collect(java.util.stream.Collectors.joining("\n", "", "\n")));
        if (!acceptedMainFingerprint.equals(anchor.mainTreeSha256())) {
            throw new IllegalArgumentException(
                "Mixed V1 gameplay-tree fingerprint mismatch: " + acceptedMainFingerprint
            );
        }

        final var mixedV1ByPath = new LinkedHashMap<String, String>();
        mixedV1Entries.forEach(entry -> mixedV1ByPath.put(entry.relativePath(), entry.sha256()));

        for (final var entry : mixedV1Entries) {
            if (CHANGED_MAIN_PATHS.contains(entry.relativePath())
                || CHANGED_NON_MAIN_PATHS.contains(entry.relativePath())) {
                safeRegularFile(root.resolve(entry.relativePath()), root, "declared mixed V2 delta");
            } else {
                verifyEntry(root, entry);
            }
        }

        final var missingDeclaredNonMain = new TreeSet<>(CHANGED_NON_MAIN_PATHS);
        missingDeclaredNonMain.removeAll(mixedV1ByPath.keySet());
        if (!missingDeclaredNonMain.isEmpty()) {
            throw new IllegalArgumentException(
                "Mixed V1 manifest is missing declared non-main inputs: " + missingDeclaredNonMain
            );
        }
        final var changedNonMain = new TreeSet<String>();
        for (final var relativePath : CHANGED_NON_MAIN_PATHS) {
            final var input = safeRegularFile(
                root.resolve(relativePath),
                root,
                "declared mixed V2 non-main delta"
            );
            if (!fileSha256(input).equals(mixedV1ByPath.get(relativePath))) {
                changedNonMain.add(relativePath);
            }
        }
        if (!changedNonMain.equals(CHANGED_NON_MAIN_PATHS)) {
            throw new IllegalArgumentException(
                "SCALING_V3 changed an unexpected set of declared non-main files: " + changedNonMain
            );
        }

        final var currentMain = regularFiles(root, "game/src/main");
        final var expectedCurrentMain = new TreeSet<>(acceptedMain.keySet());
        expectedCurrentMain.addAll(ADDED_MAIN_PATHS);
        if (!expectedCurrentMain.equals(currentMain)) {
            final var missing = new TreeSet<>(expectedCurrentMain);
            missing.removeAll(currentMain);
            final var extra = new TreeSet<>(currentMain);
            extra.removeAll(expectedCurrentMain);
            throw new IllegalArgumentException(
                "SCALING_V3 main tree has an unexpected path delta: missing=" + missing
                    + "; extra=" + extra
            );
        }

        final var changedMain = new TreeSet<String>();
        acceptedMain.forEach((relativePath, acceptedSha256) -> {
            final var input = safeRegularFile(root.resolve(relativePath), root, "mixed V2 gameplay input");
            if (!fileSha256(input).equals(acceptedSha256)) {
                changedMain.add(relativePath);
            }
        });
        if (!changedMain.equals(CHANGED_MAIN_PATHS)) {
            throw new IllegalArgumentException(
                "SCALING_V3 changed an unexpected set of existing files: " + changedMain
            );
        }
        ADDED_MAIN_PATHS.forEach(relativePath ->
            safeRegularFile(root.resolve(relativePath), root, "added SCALING_V3 input")
        );
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
        fixed.addAll(MIXED_V1_EVIDENCE_PATHS);
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
        return lines.stream().map(Step12MixedV2InputManifest::parseEntry).toList();
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

    private static void requireAbsentPath(Path root, String relativePath, String name) {
        final var path = root.resolve(relativePath).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException(name + " is outside the root: " + path);
        }
        if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException(name + " must be absent: " + path);
        }
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

    record MixedV1Anchor(String manifestFingerprint, String mainTreeSha256, String evidenceSetSha256) {
    }

    private record ManifestEntry(String sha256, String relativePath) {
    }
}
