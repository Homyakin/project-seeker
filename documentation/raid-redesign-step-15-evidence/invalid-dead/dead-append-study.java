package ru.homyakin.seeker.game.battle;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.zip.GZIPOutputStream;
import ru.homyakin.seeker.game.battle.PveAdaptationStudy.Cell;
import ru.homyakin.seeker.game.battle.PveAdaptationStudy.InputMember;
import ru.homyakin.seeker.game.battle.PveAdaptationStudy.Mode;
import ru.homyakin.seeker.game.battle.Step123ReferenceTeams.Specialization;
import ru.homyakin.seeker.game.battle.Step123ReferenceTeams.Tier;
import ru.homyakin.seeker.game.battle.targeting.TargetingTactic;
import ru.homyakin.seeker.game.event.pve.PveCharacteristicScaling;
import ru.homyakin.seeker.game.event.raid.models.RaidType;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogLoader;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogVersion;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemRarity;

/**
 * Myconid-only comparison of actual generators and Battle.process, without source overlays.
 *
 * <p>A mirrored pair shares generation and battle seeds; its two battles are not independent
 * observations. Intervals therefore use pair scores, not a binomial interval over individual
 * battles. They describe sampling of fixed seeded inputs, not universal win probabilities or
 * a requirement that every composition wins half its fights.</p>
 */
public final class MyconidAdaptationStudy {
    private static final long SEED_OFFSET = seedOffset();
    private static final long GENERATION_ROOT = Math.addExact(2_026_110_815_000L, SEED_OFFSET);
    private static final long BATTLE_ROOT = Math.addExact(3_026_110_815_000L, SEED_OFFSET);
    private static final List<Tier> TIERS = List.of(Tier.BEGINNER, Tier.MID, Tier.STRONG);
    private static final List<Integer> LEVELS = List.of(1, 10, 25, 50);
    private static final List<Integer> SCREEN_SIZES = List.of(1, 2, 3, 7, 8, 15, 30);

    private MyconidAdaptationStudy() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1 || args[0].isBlank()) {
            throw new IllegalArgumentException("Specify one new output directory");
        }
        final int pairs = Integer.getInteger("myconid.study.pairs", 20);
        if (pairs < 1 || pairs > 1000) {
            throw new IllegalArgumentException("Pairs must be in 1..1000");
        }
        final var suite = System.getProperty("myconid.study.suite", "full");
        if (!List.of("screen", "full", "causal").contains(suite)) {
            throw new IllegalArgumentException("Suite must be screen, full or causal");
        }
        final var phase = System.getProperty("myconid.study.phase", "UNSPECIFIED");
        final var extraStyles = extraStyles();
        final var output = Path.of(args[0]);
        Files.createDirectory(output);
        if (suite.equals("causal")) {
            runCausal(output, pairs, phase);
            return;
        }
        final var allCases = cases(extraStyles);
        final var requestedCases = integerListProperty("myconid.study.caseIds", 1, allCases.size());
        final var summaries = new ArrayList<Map<String, Object>>();
        final var rawPath = output.resolve("battles.jsonl.gz");
        int caseId = 0;
        try (var raw = writer(rawPath)) {
            for (final var cell : allCases) {
                ++caseId;
                if (!requestedCases.isEmpty() && !requestedCases.contains(caseId)) {
                    continue;
                }
                if (suite.equals("screen") && !isScreen(cell)) {
                    continue;
                }
                if (Boolean.getBoolean("myconid.study.onlyMain") && !cell.style().equals("MAIN")) {
                    continue;
                }
                summaries.add(runCell(cell, caseId, pairs, phase, raw));
                if (summaries.size() % 20 == 0) {
                    System.out.println("completed cells=" + summaries.size() + " lastCaseId=" + caseId);
                }
            }
        }
        final var manifest = new LinkedHashMap<String, Object>();
        manifest.put("purpose", "MYCONID_ADAPTATION_COMPARISON_NOT_UNIVERSAL_BALANCE_ACCEPTANCE");
        manifest.put("phase", phase);
        manifest.put("suite", suite);
        manifest.put("onlyMain", Boolean.getBoolean("myconid.study.onlyMain"));
        manifest.put("caseIds", requestedCases);
        manifest.put("pairs", pairs);
        manifest.put("seedOffset", SEED_OFFSET);
        manifest.put("generationRoot", GENERATION_ROOT);
        manifest.put("battleRoot", BATTLE_ROOT);
        manifest.put("caseIdPolicy", "ONE_BASED_INDEX_IN_FULL_MATRIX_STABLE_ACROSS_PHASES_AND_SCREEN_SUBSETS");
        manifest.put("seedPolicy", "ROOT + caseId * 1000 + pair; both mirrors reconstruct fresh participants");
        manifest.put("catalog", EquipmentCatalogVersion.SCALING_V5);
        manifest.put("playerSkills", PveAdaptationStudy.FORMULA);
        manifest.put("npcSkills", PveAdaptationStudy.FORMULA);
        manifest.put("tempo", PveAdaptationStudy.TEMPO);
        manifest.put("extraStyles", extraStyles);
        manifest.put("extraLevels", extraLevels());
        manifest.put("confidencePolicy", "95% Hoeffding by complete independent pairs, per cell, not simultaneous");
        manifest.put("timeoutPolicy", "TIMEOUT is not LOSS; incomplete pairs are excluded from score intervals");
        manifest.put("durationPolicy", "Rounds and action metrics summarize completed battles only");
        manifest.put("rawSha256", sha256(rawPath));
        manifest.put("cases", summaries);
        Files.writeString(output.resolve("summary.json"),
            PveAdaptationStudy.JSON.writerWithDefaultPrettyPrinter().writeValueAsString(manifest),
            StandardOpenOption.CREATE_NEW);
        System.out.println("complete cells=" + summaries.size() + " battles=" + summaries.size() * pairs * 2);
    }

    private static Map<String, Object> runCell(
        Cell cell, int caseId, int pairs, String phase, BufferedWriter raw
    ) throws Exception {
        return runCell(cell, caseId, pairs, phase, raw, null, caseId, Map.of());
    }

    private static Map<String, Object> runCell(
        Cell cell, int caseId, int pairs, String phase, BufferedWriter raw, List<InputMember> supplied,
        long seedCaseId, Map<String, Object> metadata
    ) throws Exception {
        final var outcomes = new TreeMap<String, Integer>();
        final var outcomesByOrder = new TreeMap<String, Map<String, Integer>>();
        final var metrics = new TreeMap<String, List<Double>>();
        final var calibrationStatuses = new TreeMap<String, Integer>();
        final var relativeErrors = new ArrayList<Double>();
        final var actualPositions = new TreeMap<String, Integer>();
        final var pairScores = new ArrayList<Double>();
        final var pairScoresByIndex = new ArrayList<Double>();
        int incompletePairs = 0;
        for (int pair = 0; pair < pairs; pair++) {
            Map<String, Object> first = null;
            double pairScore = 0;
            boolean incomplete = false;
            for (final boolean reverse : List.of(false, true)) {
                final var seedStep = Math.addExact(Math.multiplyExact(seedCaseId, 1000L), pair);
                final var generationSeed = Math.addExact(GENERATION_ROOT, seedStep);
                final var battleSeed = Math.addExact(BATTLE_ROOT, seedStep);
                final var row = supplied == null
                    ? PveAdaptationStudy.run(cell, generationSeed, battleSeed, reverse)
                    : PveAdaptationStudy.runWithMembers(cell, supplied, generationSeed, battleSeed, reverse);
                row.put("phase", phase);
                row.put("caseId", caseId);
                row.put("pair", pair);
                row.putAll(metadata);
                if (first == null) {
                    first = row;
                } else {
                    assertMirroredInputs(first, row);
                }
                raw.write(PveAdaptationStudy.JSON.writeValueAsString(row));
                raw.newLine();
                final var outcome = (String) row.get("outcome");
                outcomes.merge(outcome, 1, Integer::sum);
                outcomesByOrder.computeIfAbsent(reverse ? "PLAYERS_FIRST" : "ENEMIES_FIRST",
                    unused -> new TreeMap<>()).merge(outcome, 1, Integer::sum);
                if (outcome.equals("TIMEOUT")) {
                    incomplete = true;
                } else {
                    pairScore += outcome.equals("WIN") ? 0.5 : outcome.equals("DRAW") ? 0.25 : 0.0;
                    collectMetrics(metrics, row);
                    for (final var position : (List<?>) row.get("actualEnemyPositions")) {
                        actualPositions.merge(position.toString(), 1, Integer::sum);
                    }
                }
                final var calibration = (PveCharacteristicScaling.Result) row.get("calibration");
                calibrationStatuses.merge(calibration.status().name(), 1, Integer::sum);
                if (calibration.targetPower() > 0) {
                    relativeErrors.add(Math.abs(calibration.achievedPower() / calibration.targetPower() - 1));
                }
            }
            if (incomplete) {
                ++incompletePairs;
                pairScoresByIndex.add(null);
            } else {
                pairScores.add(pairScore);
                pairScoresByIndex.add(pairScore);
            }
        }
        final var result = new LinkedHashMap<String, Object>();
        result.put("caseId", caseId);
        result.put("cell", cell);
        result.putAll(metadata);
        result.put("outcomes", outcomes);
        result.put("outcomesByOrder", outcomesByOrder);
        result.put("attemptedBattles", 2 * pairs);
        result.put("completedBattles", 2 * pairs - outcomes.getOrDefault("TIMEOUT", 0));
        result.put("playerWinsPerAttempt", (double) outcomes.getOrDefault("WIN", 0) / (2 * pairs));
        result.put("completePairs", pairScores.size());
        result.put("incompletePairs", incompletePairs);
        result.put("pairScores", pairScores);
        result.put("pairScoresByIndex", pairScoresByIndex);
        if (!pairScores.isEmpty()) {
            final double mean = pairScores.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
            final double halfWidth = Math.sqrt(Math.log(40.0) / (2.0 * pairScores.size()));
            result.put("pairedScoreMean", mean);
            result.put("pairedScore95Hoeffding", Map.of(
                "low", Math.max(0, mean - halfWidth), "high", Math.min(1, mean + halfWidth),
                "unclippedHalfWidth", halfWidth));
        }
        final var metricSummaries = new TreeMap<String, Object>();
        metrics.forEach((key, values) -> metricSummaries.put(key, distribution(values)));
        result.put("completedBattleMetrics", metricSummaries);
        result.put("actualEnemyPositionCounts", actualPositions);
        result.put("calibrationStatuses", calibrationStatuses);
        result.put("calibrationAbsoluteRelativeError", distribution(relativeErrors));
        return result;
    }

    private static void runCausal(Path output, int pairs, String phase) throws Exception {
        final var rawPath = output.resolve("battles.jsonl.gz");
        final var summaries = new ArrayList<Map<String, Object>>();
        final var comparisons = new ArrayList<Map<String, Object>>();
        final var skipped = new ArrayList<Map<String, Object>>();
        final var requestedGroups = java.util.Arrays.stream(System.getProperty("myconid.study.causalGroupIds", "")
                .split(",")).map(String::trim).filter(value -> !value.isEmpty()).map(Integer::valueOf).toList();
        if (requestedGroups.stream().anyMatch(group -> group < 1 || group > 24)) {
            throw new IllegalArgumentException("Causal group IDs must be in 1..24");
        }
        final var allVariants = List.of("BASE", "NORMAL_APPEND", "WEAK_APPEND", "SINGLE_ITEM_ENHANCE",
            "SINGLE_MODIFIER_RARITY");
        final var requestedVariants = java.util.Arrays.stream(System.getProperty("myconid.study.causalVariants",
                String.join(",", allVariants)).split(","))
            .map(String::trim).filter(value -> !value.isEmpty()).toList();
        if (!requestedVariants.contains("BASE") || !allVariants.containsAll(requestedVariants)) {
            throw new IllegalArgumentException("Causal variants must include BASE and use only " + allVariants);
        }
        final var sword = EquipmentCatalogLoader.loadValidated(EquipmentCatalogVersion.SCALING_V5).itemObjects()
            .stream().filter(item -> item.code().equals("sword")).findFirst().orElseThrow();
        int caseId = 0;
        int groupId = 0;
        int completedGroups = 0;
        try (var raw = writer(rawPath)) {
            for (final var tier : List.of(Tier.MID, Tier.STRONG)) {
                for (final int size : List.of(1, 2, 3, 7, 8, 15)) {
                    for (final int level : List.of(1, 10)) {
                        ++groupId;
                        if (!requestedGroups.isEmpty() && !requestedGroups.contains(groupId)) {
                            continue;
                        }
                        final var baseCell = cell("MAIN", tier, size, level, false);
                        final var baseMembers = PveAdaptationStudy.inputFor(baseCell);
                        final var variants = new ArrayList<Variant>();
                        variants.add(new Variant("BASE", baseMembers, "Unchanged main reference input"));
                        final var nextCanonical = PveAdaptationStudy.inputFor(cell("MAIN", tier, size + 1, level, false))
                            .get(size);
                        variants.add(new Variant("NORMAL_APPEND", append(baseMembers, nextCanonical),
                            "Append only the next canonical member; retain every existing item, position and tactic"));
                        final var weak = new InputMember("WEAK_SWORD_ONLY", Position.FRONT, TargetingTactic.THREAT,
                            List.of(new Item(sword, Optional.empty(), ItemRarity.COMMON, tier.level())));
                        variants.add(new Variant("WEAK_APPEND", append(baseMembers, weak),
                            "Append common V5 sword only at the same enhancement; no armor, offhand or modifiers"));
                        variants.add(enhanceOne(baseMembers));
                        final var rarity = raiseOneRarity(baseMembers);
                        if (rarity.isPresent()) {
                            variants.add(rarity.get());
                        } else {
                            skipped.add(Map.of("causalGroupId", groupId, "cell", baseCell,
                                "variant", "SINGLE_MODIFIER_RARITY", "reason", "NO_MODIFIER_BELOW_LEGENDARY"));
                        }
                        Map<String, Object> baseSummary = null;
                        int variantIndex = 0;
                        for (final var variant : variants) {
                            caseId = (groupId - 1) * 5 + ++variantIndex;
                            if (!requestedVariants.contains(variant.name())) {
                                continue;
                            }
                            final var variantCell = cell("CAUSAL_" + variant.name(), tier,
                                variant.members().size(), level, false);
                            final var summary = runCell(variantCell, caseId, pairs, phase, raw, variant.members(),
                                10_000L + groupId, Map.of("causalGroupId", groupId, "variant", variant.name(),
                                    "sourceChange", variant.change(), "baseCell", baseCell));
                            summaries.add(summary);
                            if (baseSummary == null) {
                                baseSummary = summary;
                            } else {
                                comparisons.add(causalComparison(baseSummary, summary));
                            }
                        }
                        ++completedGroups;
                        System.out.println("completed causal groups=" + completedGroups + " cells=" + summaries.size());
                    }
                }
            }
        }
        final var manifest = new LinkedHashMap<String, Object>();
        manifest.put("purpose", "CONTROLLED_SAME_OLD_MEMBERS_COMPARISON_NOT_CANONICAL_TEAM_SIZE_COMPARISON");
        manifest.put("phase", phase);
        manifest.put("suite", "causal");
        manifest.put("causalGroupIds", requestedGroups);
        manifest.put("causalVariants", requestedVariants);
        manifest.put("caseIdPolicy", "Five reserved variants per stable group; filtering does not change IDs or seeds");
        manifest.put("pairs", pairs);
        manifest.put("seedOffset", SEED_OFFSET);
        manifest.put("generationRoot", GENERATION_ROOT);
        manifest.put("battleRoot", BATTLE_ROOT);
        manifest.put("seedPolicy", "ROOT + (10000 + causalGroupId) * 1000 + pair, shared by all group variants");
        manifest.put("couplingCaveat", "Adding a member consumes IDs and changes enemy IDs; seeds do not promise identical random draws");
        manifest.put("confidencePolicy", "Per-cell 95% Hoeffding; paired-delta range [-1,1], not simultaneous");
        manifest.put("timeoutPolicy", "TIMEOUT is not LOSS; comparisons use only indices complete in both variants");
        manifest.put("catalog", EquipmentCatalogVersion.SCALING_V5);
        manifest.put("playerSkills", PveAdaptationStudy.FORMULA);
        manifest.put("npcSkills", PveAdaptationStudy.FORMULA);
        manifest.put("tempo", PveAdaptationStudy.TEMPO);
        manifest.put("rawSha256", sha256(rawPath));
        manifest.put("cases", summaries);
        manifest.put("comparisons", comparisons);
        manifest.put("skippedVariants", skipped);
        Files.writeString(output.resolve("summary.json"),
            PveAdaptationStudy.JSON.writerWithDefaultPrettyPrinter().writeValueAsString(manifest),
            StandardOpenOption.CREATE_NEW);
        System.out.println("complete causal groups=" + completedGroups + " cells=" + summaries.size()
            + " battles=" + summaries.size() * pairs * 2);
    }

    private static List<InputMember> append(List<InputMember> base, InputMember added) {
        final var result = new ArrayList<>(base);
        result.add(added);
        if (!result.subList(0, base.size()).equals(base)) {
            throw new IllegalStateException("Appending a participant changed the original input");
        }
        return List.copyOf(result);
    }

    private static Variant enhanceOne(List<InputMember> base) {
        final var old = base.getFirst().items().getFirst();
        final var replacement = new Item(old.object(), old.modifier(), old.rarity(), old.enhanceLevel() + 1);
        return replaceOne("SINGLE_ITEM_ENHANCE", base, 0, 0, replacement,
            "Enhance only member 0 item " + old.object().code() + " from +" + old.enhanceLevel()
                + " to +" + replacement.enhanceLevel());
    }

    private static Optional<Variant> raiseOneRarity(List<InputMember> base) {
        for (int memberIndex = 0; memberIndex < base.size(); memberIndex++) {
            final var items = base.get(memberIndex).items();
            for (int itemIndex = 0; itemIndex < items.size(); itemIndex++) {
                final var old = items.get(itemIndex);
                if (old.modifier().isPresent() && old.rarity().next().isPresent()) {
                    final var replacement = new Item(old.object(), old.modifier(), old.rarity().next().orElseThrow(),
                        old.enhanceLevel());
                    return Optional.of(replaceOne("SINGLE_MODIFIER_RARITY", base, memberIndex, itemIndex, replacement,
                        "Raise only member " + memberIndex + " item " + old.object().code() + " rarity from "
                            + old.rarity() + " to " + replacement.rarity() + "; keep modifier and enhancement"));
                }
            }
        }
        return Optional.empty();
    }

    private static Variant replaceOne(
        String name, List<InputMember> base, int memberIndex, int itemIndex, Item replacement, String change
    ) {
        final var old = base.get(memberIndex);
        final var items = new ArrayList<>(old.items());
        items.set(itemIndex, replacement);
        final var members = new ArrayList<>(base);
        members.set(memberIndex, new InputMember(old.build(), old.position(), old.tactic(), items));
        return new Variant(name, members, change);
    }

    private static Map<String, Object> causalComparison(Map<String, Object> base, Map<String, Object> variant) {
        final var baseScores = (List<?>) base.get("pairScoresByIndex");
        final var variantScores = (List<?>) variant.get("pairScoresByIndex");
        final var deltas = new ArrayList<Double>();
        for (int index = 0; index < baseScores.size(); index++) {
            if (baseScores.get(index) != null && variantScores.get(index) != null) {
                deltas.add(((Number) variantScores.get(index)).doubleValue()
                    - ((Number) baseScores.get(index)).doubleValue());
            }
        }
        final var result = new LinkedHashMap<String, Object>();
        result.put("causalGroupId", base.get("causalGroupId"));
        result.put("baseCell", base.get("baseCell"));
        result.put("baseCaseId", base.get("caseId"));
        result.put("variantCaseId", variant.get("caseId"));
        result.put("variant", variant.get("variant"));
        result.put("pairedScoreDeltas", deltas);
        result.put("distribution", distribution(deltas));
        if (!deltas.isEmpty()) {
            final double mean = deltas.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
            final double halfWidth = Math.sqrt(2.0 * Math.log(40.0) / deltas.size());
            result.put("pairedDelta95Hoeffding", Map.of("low", Math.max(-1, mean - halfWidth),
                "high", Math.min(1, mean + halfWidth), "unclippedHalfWidth", halfWidth));
        }
        return result;
    }

    private static void assertMirroredInputs(Map<String, Object> first, Map<String, Object> mirrored) {
        for (final var key : List.of("members", "sourceEffects", "combatEffects", "calibration", "playerPower",
            "enemyPower", "playerBasis", "enemyBasis")) {
            if (!PveAdaptationStudy.JSON.valueToTree(first.get(key))
                .equals(PveAdaptationStudy.JSON.valueToTree(mirrored.get(key)))) {
                throw new IllegalStateException("Mirrored battles have different generation input: " + key);
            }
        }
    }

    private static void collectMetrics(Map<String, List<Double>> metrics, Map<String, Object> row) {
        for (final var key : List.of("rounds", "actionCount", "normalAttempts", "moves", "forcedMoves")) {
            addMetric(metrics, key, (Number) row.get(key));
        }
        for (final var side : List.of("playerTotals", "enemyTotals")) {
            final var totals = (Map<?, ?>) row.get(side);
            for (final var key : List.of("survivors", "initialHealth", "remainHealth", "normalAttacksHit",
                "normalDamage", "critDamage", "skillDamage", "turns")) {
                addMetric(metrics, side + "." + key, (Number) totals.get(key));
            }
        }
    }

    private static void addMetric(Map<String, List<Double>> metrics, String key, Number value) {
        metrics.computeIfAbsent(key, unused -> new ArrayList<>()).add(value.doubleValue());
    }

    private static Map<String, Object> distribution(List<Double> values) {
        if (values.isEmpty()) {
            return Map.of("count", 0);
        }
        final var sorted = values.stream().sorted().toList();
        return Map.of("count", sorted.size(), "mean", values.stream().mapToDouble(Double::doubleValue)
                .average().orElseThrow(), "min", sorted.getFirst(), "p50", percentile(sorted, 0.5),
            "p90", percentile(sorted, 0.9), "p95", percentile(sorted, 0.95), "max", sorted.getLast());
    }

    private static double percentile(List<Double> sorted, double quantile) {
        return sorted.get(Math.max(0, (int) Math.ceil(quantile * sorted.size()) - 1));
    }

    static List<Cell> cases(List<String> extraStyles) {
        final var result = new ArrayList<Cell>();
        final var sizes = new ArrayList<Integer>();
        for (int size = 1; size <= 15; size++) {
            sizes.add(size);
        }
        sizes.add(30);
        for (final var tier : TIERS) {
            for (final int size : sizes) {
                for (final int level : LEVELS) {
                    result.add(cell("MAIN", tier, size, level, false));
                }
            }
        }
        for (final var tier : TIERS) {
            for (final var style : Specialization.values()) {
                for (final int level : List.of(1, 10, 50)) {
                    result.add(cell(style.name(), tier, 3, level, false));
                }
            }
            for (final int size : List.of(3, 15)) {
                for (final int level : List.of(1, 10, 50)) {
                    result.add(cell("MAIN", tier, size, level, true));
                }
            }
        }
        for (final var style : extraStyles) {
            for (final var tier : TIERS) {
                for (final int size : List.of(1, 3, 7, 15)) {
                    for (final int level : List.of(1, 10, 50)) {
                        result.add(cell(style, tier, size, level, false));
                    }
                }
            }
        }
        final var extraLevelStyles = new ArrayList<String>();
        extraLevelStyles.add("MAIN");
        extraLevelStyles.addAll(extraStyles);
        for (final var style : extraLevelStyles) {
            for (final var tier : TIERS) {
                for (final int size : List.of(3, 7, 15)) {
                    for (final int level : extraLevels()) {
                        result.add(cell(style, tier, size, level, false));
                    }
                }
            }
        }
        if (result.stream().distinct().count() != result.size()) {
            throw new IllegalArgumentException("Duplicate study cases");
        }
        return List.copyOf(result);
    }

    private static Cell cell(String style, Tier tier, int size, int level, boolean effects) {
        return new Cell(Mode.RAID, RaidType.MYCONID_COLONY.name(), style, tier, size, level, effects);
    }

    private static boolean isScreen(Cell cell) {
        return SCREEN_SIZES.contains(cell.size()) && cell.raidLevel() != 25;
    }

    private static List<String> extraStyles() {
        final var property = System.getProperty("myconid.study.extraStyles", "COMMAND_CUIRASS,RITUAL");
        if (property.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(property.split(",")).map(String::trim).filter(style -> !style.isEmpty()).toList();
    }

    private static List<Integer> extraLevels() {
        final var levels = integerListProperty("myconid.study.extraLevels", 1, 50);
        if (levels.stream().anyMatch(LEVELS::contains)) {
            throw new IllegalArgumentException("Extra levels must not repeat existing levels 1,10,25,50");
        }
        return levels;
    }

    private static List<Integer> integerListProperty(String name, int minimum, int maximum) {
        final var values = java.util.Arrays.stream(System.getProperty(name, "").split(","))
            .map(String::trim).filter(value -> !value.isEmpty()).map(Integer::valueOf).toList();
        if (values.stream().anyMatch(value -> value < minimum || value > maximum)) {
            throw new IllegalArgumentException(name + " values must be in " + minimum + ".." + maximum);
        }
        return values;
    }

    private static long seedOffset() {
        final var offset = Long.parseLong(System.getProperty("myconid.study.seedOffset", "0"));
        if (offset < 0) {
            throw new IllegalArgumentException("Seed offset must be nonnegative");
        }
        return offset;
    }

    private static BufferedWriter writer(Path path) throws Exception {
        return new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(
            path, StandardOpenOption.CREATE_NEW)), StandardCharsets.UTF_8));
    }

    private static String sha256(Path path) throws Exception {
        final var digest = MessageDigest.getInstance("SHA-256");
        try (var input = Files.newInputStream(path)) {
            final var buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                digest.update(buffer, 0, count);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private record Variant(String name, List<InputMember> members, String change) {
        Variant {
            members = List.copyOf(members);
        }
    }
}
