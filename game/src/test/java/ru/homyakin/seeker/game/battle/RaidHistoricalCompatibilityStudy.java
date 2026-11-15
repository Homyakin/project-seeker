package ru.homyakin.seeker.game.battle;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.IntStream;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;
import ru.homyakin.seeker.game.event.raid.generator.RaidBattlePersonageGenerator;
import ru.homyakin.seeker.game.event.raid.models.RaidType;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.DefenseType;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemAttack;
import ru.homyakin.seeker.game.item.models.ItemDefense;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffects;
import ru.homyakin.seeker.utils.RandomUtils;

/** Explicit binary comparison against separately saved pre-change classes; never a normal JUnit test. */
public final class RaidHistoricalCompatibilityStudy {
    private static final String PREFIX = "ru.homyakin.seeker.game.event.raid.generator.";
    private static final Map<RaidType, String> GENERATORS = Map.of(
        RaidType.WOLFPACK, PREFIX + "WolfPackGenerator",
        RaidType.ZOMBIE_HORDE, PREFIX + "ZombieHordeGenerator",
        RaidType.MYCONID_COLONY, PREFIX + "MyconidColonyGenerator",
        RaidType.MAGGEESE_FLOCK, PREFIX + "MaggeeseFlockGenerator"
    );
    private static final Set<String> CHILD_CLASSES = Set.of(
        PREFIX + "WolfPackGenerator", PREFIX + "ZombieHordeGenerator", PREFIX + "MyconidColonyGenerator",
        PREFIX + "MyconidColonyGenerator$Formation", PREFIX + "MaggeeseFlockGenerator"
    );
    private static final int[] SIZES = {1, 2, 3, 7, 8, 10, 11, 13, 14, 30, 128};
    private static final int[] LEVELS = {1, 25, 50};
    private static final long[] SEEDS = {20_261_115_171_001L, 20_261_115_171_002L};
    private static final List<SkillFormulaVersion> OLD_VERSIONS = List.of(
        SkillFormulaVersion.LEGACY_SKILLS_V1, SkillFormulaVersion.SCALING_SKILLS_V1,
        SkillFormulaVersion.SCALING_SKILLS_V2
    );
    private static final JsonMapper JSON = JsonMapper.builder().addModule(new Jdk8Module())
        .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS).build();

    private RaidHistoricalCompatibilityStudy() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2 || args[0].isBlank() || args[1].isBlank()) {
            throw new IllegalArgumentException("Specify BASELINE_CLASSES_DIRECTORY and NEW_OUTPUT_JSON");
        }
        final var baseline = Path.of(args[0]).toRealPath();
        final var output = Path.of(args[1]);
        if (Files.exists(output)) {
            throw new IllegalArgumentException("Output must not already exist: " + output);
        }
        final var baselineHashes = baselineHashes(baseline);
        final var currentHashes = currentHashes();
        final var comparisons = new ArrayList<Map<String, Object>>();
        int historicalChecks = 0;
        int myconidChecks = 0;
        try (var loader = new BaselineLoader(baseline.toUri().toURL())) {
            require(loader.loadClass(BattlePersonage.class.getName()) == BattlePersonage.class,
                "BattlePersonage must be shared, not loaded from the baseline");
            require(loader.loadClass(RandomUtils.class.getName()) == RandomUtils.class,
                "The random scope must be shared");
            require(loader.loadClass(RaidBattlePersonageGenerator.class.getName()) == RaidBattlePersonageGenerator.class,
                "The generator interface must be shared");
            for (final var type : RaidType.values()) {
                final var versions = new ArrayList<>(OLD_VERSIONS);
                if (type == RaidType.MYCONID_COLONY) {
                    versions.add(SkillFormulaVersion.SCALING_SKILLS_V3);
                }
                final var className = GENERATORS.get(type);
                final var oldClass = loader.loadClass(className);
                require(oldClass.getClassLoader() == loader, "Baseline generator fell back to current code: " + className);
                final var newClass = Class.forName(className);
                require(newClass.getClassLoader() != loader, "Current generator unexpectedly loaded from baseline");
                final var oldGenerator = (RaidBattlePersonageGenerator) oldClass.getConstructor().newInstance();
                final var newGenerator = (RaidBattlePersonageGenerator) newClass.getConstructor().newInstance();
                for (final var version : versions) {
                    for (final int size : SIZES) {
                        for (final int level : LEVELS) {
                            for (final long seed : SEEDS) {
                                final var before = run(oldGenerator, version, size, level, seed);
                                final var after = run(newGenerator, version, size, level, seed);
                                final var descriptor = Map.of("raid", type.name(), "version", version.name(),
                                    "size", size, "level", level, "seed", seed);
                                require(Arrays.equals(before, after), "Binary replay differs: " + descriptor
                                    + "; baseline=" + sha256(before) + "; current=" + sha256(after));
                                comparisons.add(Map.of("case", descriptor, "baselineSha256", sha256(before),
                                    "currentSha256", sha256(after), "serializedBytes", before.length));
                                if (version == SkillFormulaVersion.SCALING_SKILLS_V3) {
                                    ++myconidChecks;
                                } else {
                                    ++historicalChecks;
                                }
                            }
                        }
                    }
                }
            }
            require(loader.loadClass(PREFIX + "MyconidColonyGenerator$Formation").getClassLoader() == loader,
                "Baseline nested formation must be loaded by the baseline loader");
        }
        require(historicalChecks == 792 && myconidChecks == 66, "Unexpected matrix counts");
        require(baselineHashes.equals(baselineHashes(baseline)), "Baseline class files changed during replay");
        require(currentHashes.equals(currentHashes()), "Current class files changed during replay");
        final var report = new LinkedHashMap<String, Object>();
        report.put("purpose", "BINARY_COMPATIBILITY_NOT_BATTLE_BALANCE_ACCEPTANCE");
        report.put("baselineDirectory", baseline.toString());
        report.put("baselineClassSha256", baselineHashes);
        report.put("currentClassSha256", currentHashes);
        report.put("childFirstClasses", CHILD_CLASSES.stream().sorted().toList());
        report.put("normalization", "Map keys only; all arrays and encounter order remain unchanged");
        report.put("historicalChecks", historicalChecks);
        report.put("myconidV3Checks", myconidChecks);
        report.put("totalChecks", comparisons.size());
        report.put("comparisonsSha256", sha256(JSON.writeValueAsBytes(comparisons)));
        report.put("comparisons", comparisons);
        final var bytes = JSON.writerWithDefaultPrettyPrinter().writeValueAsBytes(report);
        Files.write(output, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        System.out.println("binary replay successful: historical=" + historicalChecks + ", myconidV3=" + myconidChecks
            + ", total=" + comparisons.size() + ", reportSha256=" + sha256(bytes));
    }

    private static byte[] run(
        RaidBattlePersonageGenerator generator, SkillFormulaVersion version, int size, int level, long seed
    ) {
        return RandomUtils.withSeed(seed, () -> {
            final var players = players(size);
            final var bonus = 1.0 + ((level - 10) * 0.06 + 0.14);
            final var generated = generator.generateCalibrated(players, bonus, version);
            final var nextDraw = RandomUtils.getInInterval(0L, Long.MAX_VALUE - 1);
            final var snapshot = new LinkedHashMap<String, Object>();
            snapshot.put("calibration", generated.calibration());
            snapshot.put("diagnostics", generated.diagnostics());
            snapshot.put("enemies", generated.enemies().stream().map(RaidHistoricalCompatibilityStudy::snapshot).toList());
            snapshot.put("nextRandomLong", nextDraw);
            try {
                return JSON.writeValueAsBytes(snapshot);
            } catch (IOException error) {
                throw new IllegalStateException("Could not serialize compatibility snapshot", error);
            }
        });
    }

    private static Map<String, Object> snapshot(BattlePersonage personage) {
        final var result = new LinkedHashMap<String, Object>();
        result.put("id", personage.id());
        result.put("name", personage.name());
        result.put("stats", personage.battlePersonageStats());
        result.put("health", personage.health());
        result.put("maxHealth", personage.maxHealth());
        result.put("alive", personage.isAlive());
        result.put("startPosition", personage.startPosition());
        result.put("currentPosition", personage.currentPosition());
        result.put("range", personage.range());
        result.put("attackTypes", personage.attackTypes());
        result.put("attacksByRange", personage.attacksByRange());
        result.put("defenses", personage.defenses());
        result.put("defenseReductions", personage.defenseReductions());
        result.put("critChance", personage.critChance());
        result.put("dodgeChance", personage.dodgeChance());
        result.put("critMultiplierBits", bits(personage.critMultiplier()));
        result.put("initiative", personage.initiative());
        result.put("initiativeGauge", personage.initiativeGauge());
        result.put("readyToAct", personage.readyToAct());
        result.put("threat", personage.totalThreat());
        result.put("impactStrength", personage.impactStrength());
        result.put("skills", personage.skillSnapshots());
        result.put("items", personage.itemSnapshots());
        result.put("tactic", personage.targetingTactic());
        result.put("tempo", personage.battleTempoVersion());
        result.put("legacyPowerBits", bits(personage.legacyPower()));
        result.put("equipmentPowerBits", bits(personage.equipmentPowerUnscaled()));
        return result;
    }

    private static List<BattlePersonage> players(int count) {
        return IntStream.range(0, count).mapToObj(index -> BattlePersonage.forCombat(List.of(Item.fromObject(
            new ItemObject(null, Set.of(PersonageSlot.MAIN_HAND),
                Optional.of(new ItemAttack(AttackType.values()[index % AttackType.values().length], 1, 500 + index * 17)),
                Optional.of(new ItemDefense(DefenseType.values()[index % DefenseType.values().length], 300 + index * 5)),
                4000 + index * 31, 5 + index % 9, 3 + index % 7, 0.4, 200 + index % 300, 10, null))),
            Position.FRONT, PersonageEffects.EMPTY, Optional.empty(), SkillFormulaVersion.SCALING_SKILLS_V3)).toList();
    }

    private static Map<String, String> baselineHashes(Path root) throws IOException {
        final var result = new TreeMap<String, String>();
        for (final var name : CHILD_CLASSES) {
            result.put(name, sha256(Files.readAllBytes(root.resolve(classPath(name)))));
        }
        return result;
    }

    private static Map<String, String> currentHashes() throws IOException {
        final var result = new TreeMap<String, String>();
        for (final var name : CHILD_CLASSES) {
            try (var input = RaidHistoricalCompatibilityStudy.class.getClassLoader().getResourceAsStream(classPath(name))) {
                if (input == null) {
                    throw new IOException("Missing current class: " + name);
                }
                result.put(name, sha256(input.readAllBytes()));
            }
        }
        return result;
    }

    private static String classPath(String name) {
        return name.replace('.', '/') + ".class";
    }

    private static String bits(double value) {
        return Long.toHexString(Double.doubleToRawLongBits(value));
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (java.security.NoSuchAlgorithmException error) {
            throw new IllegalStateException(error);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    /** Exactly five child-first classes; every model, interface and random scope comes from the parent. */
    private static final class BaselineLoader extends URLClassLoader {
        private BaselineLoader(URL directory) {
            super(new URL[]{directory}, RaidHistoricalCompatibilityStudy.class.getClassLoader());
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            synchronized (getClassLoadingLock(name)) {
                if (!CHILD_CLASSES.contains(name)) {
                    return super.loadClass(name, resolve);
                }
                var loaded = findLoadedClass(name);
                if (loaded == null) {
                    loaded = findClass(name);
                }
                if (resolve) {
                    resolveClass(loaded);
                }
                return loaded;
            }
        }
    }
}
