package ru.homyakin.seeker.game.battle;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.EnumSet;
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
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.utils.RandomUtils;

/** Fixed step 15.7 sample of the actual current generator and Battle.process, without tuning. */
public final class Step157MyconidValidationStudy {
    static final long GENERATION_ROOT = 20_261_115_157_000L;
    static final long BATTLE_ROOT = 30_261_115_157_000L;
    static final String METRIC = "EQUIPMENT_POWER_SUM_V1";
    private static final List<Tier> TIERS = List.of(Tier.BEGINNER, Tier.MID, Tier.STRONG);
    private static final List<Integer> LEVELS = List.of(1, 10, 25, 40, 50);
    private static final List<Integer> SIZES = List.of(1, 2, 3, 7, 8, 15, 30);
    private static final List<String> VARIANTS = List.of("BASE", "NORMAL_APPEND", "WEAK_APPEND",
        "ONE_ENHANCE", "ONE_RARITY", "UNEQUIP_ARMOR");
    private static final List<Integer> REPLAY_CASE_IDS = List.of(1, 5, 35, 70, 105, 106, 142, 249,
        250, 267, 268, 270, 273, 339, 340, 359);

    private Step157MyconidValidationStudy() {
    }

    /** Run from the repository root: plan DIR, then run DIR. Neither command overwrites evidence. */
    public static void main(String[] args) throws Exception {
        if ((args.length != 2 || !List.of("plan", "run").contains(args[0]))
            && (args.length != 3 || !args[0].equals("replay"))) {
            throw new IllegalArgumentException("Expected plan|run DIR, or replay ORIGINAL_DIR NEW_DIR");
        }
        final var output = Path.of(args[1]);
        final var cases = cases();
        final var protocol = protocol(cases);
        final var protocolPath = output.resolve("protocol.json");
        if (args[0].equals("plan")) {
            Files.createDirectory(output);
            writeJson(protocolPath, protocol);
            System.out.println("planned cells=" + cases.size() + " battles=" + battleCount(cases)
                + " protocolSha256=" + sha256(protocolPath));
            return;
        }
        if (!PveAdaptationStudy.JSON.readTree(protocolPath.toFile())
            .equals(PveAdaptationStudy.JSON.valueToTree(protocol))) {
            throw new IllegalStateException("The pre-battle protocol, inputs or public sources changed");
        }
        if (args[0].equals("replay")) {
            final var replay = Path.of(args[2]);
            Files.createDirectory(replay);
            try (var raw = writer(replay.resolve("battles.jsonl.gz"))) {
                for (final var sample : cases) {
                    if (REPLAY_CASE_IDS.contains(sample.caseId())) {
                        runCase(sample, raw, 1);
                    }
                }
            }
            System.out.println("replayed cells=" + REPLAY_CASE_IDS.size() + " battles=" + REPLAY_CASE_IDS.size() * 2);
            return;
        }
        final var summaries = new ArrayList<Map<String, Object>>();
        final var rawPath = output.resolve("battles.jsonl.gz");
        try (var raw = writer(rawPath)) {
            for (final var sample : cases) {
                summaries.add(runCase(sample, raw, sample.pairs()));
                if (sample.caseId() % 10 == 0 || sample.caseId() == cases.size()) {
                    raw.flush();
                    System.out.println("completed cells=" + sample.caseId() + "/" + cases.size());
                }
            }
        }
        final var summary = new LinkedHashMap<String, Object>();
        summary.put("purpose", "FIXED_CURRENT_EQUIPMENT_SUM_BATTLE_DIAGNOSTIC_NOT_50_PERCENT_TARGET");
        summary.put("protocolSha256", sha256(protocolPath));
        summary.put("rawSha256", sha256(rawPath));
        summary.put("caseCount", cases.size());
        summary.put("attemptedBattles", battleCount(cases));
        summary.put("cases", summaries);
        summary.put("comparisons", comparisons(cases, summaries));
        writeJson(output.resolve("summary.json"), summary);
        System.out.println("complete cells=" + cases.size() + " battles=" + battleCount(cases));
    }

    static List<Sample> cases() {
        final var result = new ArrayList<Sample>();
        final var mainSeeds = new TreeMap<String, Integer>();
        int seedGroup = 0;
        for (final var tier : TIERS) {
            for (final int size : SIZES) {
                for (final int level : LEVELS) {
                    final var cell = cell("MAIN", tier, size, level, false);
                    mainSeeds.put(context(tier, size, level), ++seedGroup);
                    add(result, "A_MAIN", "BASE", "", seedGroup, 10, cell, PveAdaptationStudy.inputFor(cell));
                }
            }
        }
        for (final var style : Specialization.values()) {
            for (final var tier : TIERS) {
                for (final int level : List.of(1, 25, 50)) {
                    final var cell = cell(style.name(), tier, 3, level, false);
                    add(result, "B_SPECIALIZED", "BASE", "", ++seedGroup, 10,
                        cell, PveAdaptationStudy.inputFor(cell));
                }
            }
        }
        for (final var style : List.of("PIERCE_ARCANE", "RAPIER_ARCANE", "COMMAND_CUIRASS", "RITUAL")) {
            for (final var tier : TIERS) {
                for (final int size : List.of(1, 3, 15)) {
                    for (final int level : List.of(1, 25, 50)) {
                        final var cell = cell(style, tier, size, level, false);
                        add(result, "C_COUNTERPLAY", "BASE", "", ++seedGroup, 10,
                            cell, PveAdaptationStudy.inputFor(cell));
                    }
                }
            }
        }
        for (final var tier : TIERS) {
            for (final int size : List.of(3, 15)) {
                for (final int level : List.of(1, 25, 50)) {
                    final var cell = cell("MAIN", tier, size, level, true);
                    add(result, "D_EFFECTS", "EFFECTS", "ATK_PLUS_20_PERCENT_HP_PLUS_15_PERCENT",
                        mainSeeds.get(context(tier, size, level)), 10, cell, PveAdaptationStudy.inputFor(cell));
                }
            }
        }
        final var weak = weakWithDefaults();
        for (final var tier : TIERS) {
            for (final int size : List.of(3, 7)) {
                for (final int level : List.of(10, 25)) {
                    ++seedGroup;
                    final var base = PveAdaptationStudy.inputFor(cell("MAIN", tier, size, level, false));
                    final var normal = PveAdaptationStudy.inputFor(cell("MAIN", tier, size + 1, level, false)).get(size);
                    for (final var variant : VARIANTS) {
                        final var changed = variant(base, normal, weak, variant);
                        add(result, "E_CAUSAL", variant, changed.change(), seedGroup, 20,
                            cell("CAUSAL_" + variant, tier, changed.members().size(), level, false), changed.members());
                    }
                }
            }
        }
        for (final int size : List.of(1, 3, 7, 15, 30)) {
            for (final int level : List.of(1, 10, 25, 50)) {
                final var members = new ArrayList<InputMember>();
                for (int index = 0; index < size; index++) {
                    members.add(new InputMember("FULL_DEFAULTS", Position.FRONT, TargetingTactic.THREAT, defaults()));
                }
                add(result, "F_FULL_DEFAULTS", "BASE", "ALL_SEVEN_ACTUAL_DEFAULT_ITEMS", ++seedGroup, 10,
                    cell("FULL_DEFAULTS", Tier.BEGINNER, size, level, false), members);
            }
        }
        if (result.size() != 359 || battleCount(result) != 8620 || seedGroup != 281) {
            throw new IllegalStateException("The fixed step 15.7 matrix changed");
        }
        return List.copyOf(result);
    }

    private static Map<String, Object> protocol(List<Sample> cases) throws Exception {
        final var result = new LinkedHashMap<String, Object>();
        result.put("protocolVersion", "STEP_15_7_FIXED_2026_11_15_V1");
        result.put("metric", METRIC);
        result.put("catalog", EquipmentCatalogVersion.SCALING_V5);
        result.put("playerSkills", PveAdaptationStudy.FORMULA);
        result.put("npcSkills", PveAdaptationStudy.FORMULA);
        result.put("tempo", PveAdaptationStudy.TEMPO);
        result.put("tempoDamageMultiplier", PveAdaptationStudy.TEMPO.damageMultiplier());
        result.put("generationRoot", GENERATION_ROOT);
        result.put("battleRoot", BATTLE_ROOT);
        result.put("seedPolicy", "root + seedGroup * 1000 + pair; mirrors share seeds and use fresh actors");
        result.put("couplingCaveat", "Added or removed members change IDs and random draws despite shared roots");
        result.put("confidencePolicy", "Per-cell 95% Hoeffding of independent complete pair scores; not simultaneous");
        result.put("timeoutPolicy", "TIMEOUT is not LOSS; incomplete pairs omitted from paired scores");
        result.put("durationPolicy", "Round/action/turn distributions include completed battles only");
        result.put("scorePolicy", "WIN=1 DRAW=0.5 LOSS=0; pair score averages its two dependent mirrors");
        result.put("acceptancePolicy", "No 50/50 target, old-rate equality, universal ally benefit or -20pp rejection");
        result.put("caseCount", cases.size());
        result.put("attemptedBattles", battleCount(cases));
        result.put("replayCaseIds", REPLAY_CASE_IDS);
        result.put("replayPairIndices", List.of(0));
        result.put("publicSourceSha256", sourceHashes());
        result.put("cases", cases.stream().map(sample -> Map.of(
            "sample", sample, "seeds", java.util.stream.IntStream.range(0, sample.pairs()).mapToObj(pair ->
                Map.of("pair", pair, "generationSeed", generationSeed(sample, pair),
                    "battleSeed", battleSeed(sample, pair))).toList())).toList());
        return result;
    }

    private static Map<String, Object> runCase(Sample sample, BufferedWriter raw, int pairs) throws Exception {
        final var outcomes = new TreeMap<String, Integer>();
        final var outcomesByOrder = new TreeMap<String, Map<String, Integer>>();
        final var metrics = new TreeMap<String, List<Double>>();
        final var calibrationStatuses = new TreeMap<String, Integer>();
        final var relativeErrors = new ArrayList<Double>();
        final var pairScores = new ArrayList<Double>();
        final var pairScoresByIndex = new ArrayList<Double>();
        for (int pair = 0; pair < pairs; pair++) {
            final long generationSeed = generationSeed(sample, pair);
            final var prepared = RandomUtils.withSeed(generationSeed,
                () -> PveAdaptationStudy.prepareMembers(sample.cell(), sample.members()));
            final var playerPowers = prepared.players().stream().map(BattlePersonage::equipmentPowerUnscaled).toList();
            final var enemyPowers = prepared.generated().enemies().stream()
                .map(BattlePersonage::equipmentPowerUnscaled).toList();
            final var diagnostics = prepared.generated().diagnostics().orElseThrow();
            final double playerSum = sortedSum(playerPowers);
            final double enemySum = sortedSum(enemyPowers);
            if (!diagnostics.assessmentVersion().equals(METRIC)
                || Double.compare(playerSum, diagnostics.participantAssessment()) != 0
                || Double.compare(enemySum, prepared.generated().calibration().achievedPower()) != 0) {
                throw new IllegalStateException("Generation did not use the current permanent equipment sum");
            }
            double score = 0;
            boolean incomplete = false;
            Map<String, Object> first = null;
            for (final boolean reverse : List.of(false, true)) {
                final var row = PveAdaptationStudy.runWithMembers(sample.cell(), sample.members(),
                    generationSeed, battleSeed(sample, pair), reverse);
                requireSame(prepared.generated().calibration(), row.get("calibration"), "prepared calibration");
                row.put("legacyDiagnosticPlayerPower", row.remove("playerPower"));
                row.put("legacyDiagnosticEnemyPower", row.remove("enemyPower"));
                row.put("playerEquipmentPowers", playerPowers);
                row.put("enemyEquipmentPowers", enemyPowers);
                row.put("playerEquipmentPowerSum", playerSum);
                row.put("enemyEquipmentPowerSum", enemySum);
                row.put("caseId", sample.caseId());
                row.put("family", sample.family());
                row.put("variant", sample.variant());
                row.put("change", sample.change());
                row.put("seedGroup", sample.seedGroup());
                row.put("pair", pair);
                if (first == null) {
                    first = row;
                } else {
                    for (final var key : List.of("members", "sourceEffects", "combatEffects", "calibration",
                        "calibrationMetric", "generationDiagnostics", "playerBasis", "enemyBasis",
                        "playerEquipmentPowers", "enemyEquipmentPowers")) {
                        requireSame(first.get(key), row.get(key), "mirrored " + key);
                    }
                }
                if (!METRIC.equals(row.get("calibrationMetric"))) {
                    throw new IllegalStateException("Unexpected calibration metric");
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
                    score += outcome.equals("WIN") ? 0.5 : outcome.equals("DRAW") ? 0.25 : 0.0;
                    collectMetrics(metrics, row);
                }
                final var calibration = (PveCharacteristicScaling.Result) row.get("calibration");
                calibrationStatuses.merge(calibration.status().name(), 1, Integer::sum);
                if (calibration.targetPower() > 0) {
                    relativeErrors.add(Math.abs(calibration.achievedPower() / calibration.targetPower() - 1));
                }
            }
            pairScoresByIndex.add(incomplete ? null : score);
            if (!incomplete) {
                pairScores.add(score);
            }
        }
        final var result = new LinkedHashMap<String, Object>();
        result.put("caseId", sample.caseId());
        result.put("family", sample.family());
        result.put("variant", sample.variant());
        result.put("change", sample.change());
        result.put("cell", sample.cell());
        result.put("seedGroup", sample.seedGroup());
        result.put("outcomes", outcomes);
        result.put("outcomesByOrder", outcomesByOrder);
        result.put("attemptedBattles", 2 * pairs);
        result.put("completedBattles", 2 * pairs - outcomes.getOrDefault("TIMEOUT", 0));
        result.put("completePairs", pairScores.size());
        result.put("incompletePairs", pairs - pairScores.size());
        result.put("pairScoresByIndex", pairScoresByIndex);
        result.put("playerWinsPerAttempt", (double) outcomes.getOrDefault("WIN", 0) / (2 * pairs));
        result.put("pairedScore", distribution(pairScores));
        if (!pairScores.isEmpty()) {
            final double mean = pairScores.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
            final double halfWidth = Math.sqrt(Math.log(40.0) / (2.0 * pairScores.size()));
            result.put("pairedScore95Hoeffding", Map.of("low", Math.max(0, mean - halfWidth),
                "high", Math.min(1, mean + halfWidth), "unclippedHalfWidth", halfWidth));
        }
        final var distributions = new TreeMap<String, Object>();
        metrics.forEach((key, values) -> distributions.put(key, distribution(values)));
        result.put("completedBattleMetrics", distributions);
        result.put("calibrationStatuses", calibrationStatuses);
        result.put("calibrationAbsoluteRelativeError", distribution(relativeErrors));
        return result;
    }

    private static List<Map<String, Object>> comparisons(List<Sample> cases, List<Map<String, Object>> summaries) {
        final var result = new ArrayList<Map<String, Object>>();
        for (final var sample : cases) {
            if (!sample.family().equals("D_EFFECTS")
                && !(sample.family().equals("E_CAUSAL") && !sample.variant().equals("BASE"))) {
                continue;
            }
            final var base = cases.stream().filter(candidate -> candidate.seedGroup() == sample.seedGroup()
                && candidate.variant().equals("BASE")).findFirst().orElseThrow();
            final var baseScores = (List<?>) summaries.get(base.caseId() - 1).get("pairScoresByIndex");
            final var changedScores = (List<?>) summaries.get(sample.caseId() - 1).get("pairScoresByIndex");
            final var deltas = new ArrayList<Double>();
            for (int pair = 0; pair < baseScores.size(); pair++) {
                if (baseScores.get(pair) != null && changedScores.get(pair) != null) {
                    deltas.add(((Number) changedScores.get(pair)).doubleValue()
                        - ((Number) baseScores.get(pair)).doubleValue());
                }
            }
            final var comparison = new LinkedHashMap<String, Object>();
            comparison.put("baseCaseId", base.caseId());
            comparison.put("variantCaseId", sample.caseId());
            comparison.put("family", sample.family());
            comparison.put("variant", sample.variant());
            comparison.put("pairedDeltas", deltas);
            comparison.put("distribution", distribution(deltas));
            if (!deltas.isEmpty()) {
                final double mean = deltas.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
                final double halfWidth = Math.sqrt(2.0 * Math.log(40.0) / deltas.size());
                comparison.put("pairedDelta95Hoeffding", Map.of("low", Math.max(-1, mean - halfWidth),
                    "high", Math.min(1, mean + halfWidth), "unclippedHalfWidth", halfWidth));
            }
            result.add(comparison);
        }
        return result;
    }

    private static Variant variant(List<InputMember> base, InputMember normal, InputMember weak, String variant) {
        final var members = new ArrayList<>(base);
        if (variant.equals("NORMAL_APPEND") || variant.equals("WEAK_APPEND")) {
            members.add(variant.equals("NORMAL_APPEND") ? normal : weak);
            return new Variant(members, "Append only " + variant + "; all old members unchanged");
        }
        if (variant.equals("BASE")) {
            return new Variant(members, "Unchanged main reference members");
        }
        int memberIndex = 0;
        int itemIndex = 0;
        boolean modifierFound = false;
        if (variant.equals("UNEQUIP_ARMOR")) {
            final var defaultCodes = defaults().stream().map(item -> item.object().code()).toList();
            itemIndex = java.util.stream.IntStream.range(0, base.getFirst().items().size())
                .filter(index -> !defaultCodes.contains(base.getFirst().items().get(index).object().code()))
                .filter(index -> base.getFirst().items().get(index).object().slots().stream()
                    .noneMatch(slot -> slot == PersonageSlot.MAIN_HAND || slot == PersonageSlot.OFF_HAND))
                .findFirst().orElseThrow();
        } else if (variant.equals("ONE_RARITY")) {
            outer:
            for (int member = 0; member < base.size(); member++) {
                for (int item = 0; item < base.get(member).items().size(); item++) {
                    final var candidate = base.get(member).items().get(item);
                    if (candidate.modifier().isPresent() && candidate.rarity().next().isPresent()) {
                        memberIndex = member;
                        itemIndex = item;
                        modifierFound = true;
                        break outer;
                    }
                }
            }
        }
        final var member = base.get(memberIndex);
        final var items = new ArrayList<>(member.items());
        final var old = items.get(itemIndex);
        final String change;
        switch (variant) {
            case "ONE_ENHANCE" -> {
                items.set(itemIndex, new Item(old.object(), old.modifier(), old.rarity(), old.enhanceLevel() + 1));
                change = "Only member0 item0 " + old.object().code() + " +" + old.enhanceLevel() + " -> +"
                    + (old.enhanceLevel() + 1);
            }
            case "ONE_RARITY" -> {
                items.set(itemIndex, new Item(old.object(), old.modifier(), old.rarity().next().orElseThrow(),
                    old.enhanceLevel()));
                change = (modifierFound ? "ONE_MODIFIER_RARITY" : "NO_MODIFIER_NEGATIVE_CONTROL")
                    + ": member" + memberIndex + " item" + itemIndex + " " + old.object().code()
                    + " " + old.rarity() + " -> " + old.rarity().next().orElseThrow();
            }
            case "UNEQUIP_ARMOR" -> {
                items.remove(itemIndex);
                fillDefaults(items);
                change = "Remove only member0 armor " + old.object().code()
                    + "; fill released slots with actual defaults; keep all weapons and other modifiers";
            }
            default -> throw new IllegalArgumentException("Unknown causal variant " + variant);
        }
        members.set(memberIndex, new InputMember(member.build(), member.position(), member.tactic(), items));
        return new Variant(members, change);
    }

    static InputMember weakWithDefaults() {
        final var catalog = EquipmentCatalogLoader.loadValidated(EquipmentCatalogVersion.SCALING_V5);
        final var sword = catalog.itemObjects().stream().filter(item -> item.code().equals("sword"))
            .findFirst().orElseThrow();
        final var items = new ArrayList<Item>();
        items.add(new Item(sword, Optional.empty(), ItemRarity.COMMON, 0));
        fillDefaults(items);
        return new InputMember("WEAK_WITH_DEFAULTS", Position.FRONT, TargetingTactic.THREAT, items);
    }

    static List<Item> defaults() {
        return EquipmentCatalogLoader.loadValidated(EquipmentCatalogVersion.SCALING_V5).defaultItems().stream()
            .map(object -> new Item(object, Optional.empty(), ItemRarity.COMMON, 0)).toList();
    }

    private static void fillDefaults(List<Item> items) {
        final var occupied = EnumSet.noneOf(PersonageSlot.class);
        items.forEach(item -> {
            for (final var slot : item.object().slots()) {
                if (!occupied.add(slot)) {
                    throw new IllegalStateException("Overlapping input equipment slots");
                }
            }
        });
        for (final var item : defaults()) {
            if (item.object().slots().stream().noneMatch(occupied::contains)) {
                items.add(item);
                occupied.addAll(item.object().slots());
            }
        }
        if (!occupied.equals(EnumSet.allOf(PersonageSlot.class))) {
            throw new IllegalStateException("Every input equipment slot must be occupied");
        }
    }

    private static void add(List<Sample> result, String family, String variant, String change,
                            int seedGroup, int pairs, Cell cell, List<InputMember> members) {
        result.add(new Sample(result.size() + 1, family, variant, change, seedGroup, pairs, cell, members));
    }

    private static Cell cell(String style, Tier tier, int size, int level, boolean effects) {
        return new Cell(Mode.RAID, RaidType.MYCONID_COLONY.name(), style, tier, size, level, effects);
    }

    private static String context(Tier tier, int size, int level) {
        return tier + ":" + size + ":" + level;
    }

    static long generationSeed(Sample sample, int pair) {
        return GENERATION_ROOT + sample.seedGroup() * 1000L + pair;
    }

    static long battleSeed(Sample sample, int pair) {
        return BATTLE_ROOT + sample.seedGroup() * 1000L + pair;
    }

    static int battleCount(List<Sample> cases) {
        return cases.stream().mapToInt(sample -> sample.pairs() * 2).sum();
    }

    private static double sortedSum(List<Double> powers) {
        double result = 0;
        for (final var power : powers.stream().sorted().toList()) {
            result += power;
        }
        if (!Double.isFinite(result) || result < 0) {
            throw new IllegalStateException("Invalid permanent equipment power sum");
        }
        return result;
    }

    private static void requireSame(Object first, Object second, String label) {
        if (!PveAdaptationStudy.JSON.valueToTree(first).equals(PveAdaptationStudy.JSON.valueToTree(second))) {
            throw new IllegalStateException("Different " + label);
        }
    }

    private static void collectMetrics(Map<String, List<Double>> metrics, Map<String, Object> row) {
        for (final var key : List.of("rounds", "actionCount", "normalAttempts", "moves", "forcedMoves")) {
            metrics.computeIfAbsent(key, unused -> new ArrayList<>()).add(((Number) row.get(key)).doubleValue());
        }
        for (final var side : List.of("playerTotals", "enemyTotals")) {
            final var totals = (Map<?, ?>) row.get(side);
            for (final var key : List.of("survivors", "initialHealth", "remainHealth", "normalAttacksHit",
                "normalDamage", "critDamage", "skillDamage", "turns")) {
                metrics.computeIfAbsent(side + "." + key, unused -> new ArrayList<>())
                    .add(((Number) totals.get(key)).doubleValue());
            }
        }
    }

    private static Map<String, Object> distribution(List<Double> values) {
        if (values.isEmpty()) {
            return Map.of("count", 0);
        }
        final var sorted = values.stream().sorted().toList();
        return Map.of("count", values.size(), "mean", values.stream().mapToDouble(Double::doubleValue)
            .average().orElseThrow(), "min", sorted.getFirst(), "p50", percentile(sorted, 0.5),
            "p90", percentile(sorted, 0.9), "p95", percentile(sorted, 0.95), "max", sorted.getLast());
    }

    private static double percentile(List<Double> sorted, double quantile) {
        return sorted.get(Math.max(0, (int) Math.ceil(quantile * sorted.size()) - 1));
    }

    private static Map<String, String> sourceHashes() throws Exception {
        final var paths = new ArrayList<String>();
        for (final var line : Files.readAllLines(Path.of("documentation/raid-redesign-step-15-6-evidence/inputs.sha256"))) {
            if (!line.isBlank()) {
                paths.add(line.substring(66));
            }
        }
        paths.add("game/src/test/java/ru/homyakin/seeker/game/battle/Step157MyconidValidationStudy.java");
        paths.add("game/src/test/java/ru/homyakin/seeker/game/battle/Step157MyconidValidationStudyTest.java");
        final var hashes = new TreeMap<String, String>();
        for (final var path : paths) {
            hashes.put(path, sha256(Path.of(path)));
        }
        return hashes;
    }

    private static BufferedWriter writer(Path path) throws Exception {
        return new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(
            path, StandardOpenOption.CREATE_NEW)), StandardCharsets.UTF_8));
    }

    private static void writeJson(Path path, Object value) throws Exception {
        Files.writeString(path, PveAdaptationStudy.JSON.writerWithDefaultPrettyPrinter().writeValueAsString(value),
            StandardOpenOption.CREATE_NEW);
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

    record Sample(int caseId, String family, String variant, String change,
                  int seedGroup, int pairs, Cell cell, List<InputMember> members) {
        Sample {
            members = List.copyOf(members);
            if (members.size() != cell.size()) {
                throw new IllegalArgumentException("Input member count differs from the declared cell");
            }
        }
    }

    private record Variant(List<InputMember> members, String change) {
        Variant {
            members = List.copyOf(members);
        }
    }
}
