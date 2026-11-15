package ru.homyakin.seeker.game.battle;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import ru.homyakin.seeker.game.battle.Step123ReferenceTeams.Tier;
import ru.homyakin.seeker.game.battle.Step123BattleRunner.Gear;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.targeting.TargetingTactic;
import ru.homyakin.seeker.game.event.pve.PveCharacteristicScaling;
import ru.homyakin.seeker.game.event.raid.models.RaidType;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogLoader;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogVersion;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.utils.RandomUtils;

/** Fixed steps 16-18 diagnostics of the actual current generators and Battle.process, without tuning. */
public final class Step1618RaidValidationStudy {
    static final long GENERATION_ROOT = 20_261_118_000_000L;
    static final long BATTLE_ROOT = 30_261_118_000_000L;
    static final String METRIC = "EQUIPMENT_POWER_SUM_V1";
    private static final List<Tier> TIERS = List.of(Tier.BEGINNER, Tier.MID, Tier.STRONG);
    private static final List<Integer> LEVELS = List.of(1, 10, 25, 50);
    private static final List<RaidType> RAIDS = List.of(RaidType.WOLFPACK, RaidType.ZOMBIE_HORDE, RaidType.MAGGEESE_FLOCK);
    private static final List<String> VARIANTS = List.of("BASE", "NORMAL_APPEND", "WEAK_APPEND",
        "ONE_ENHANCE", "ONE_RARITY", "UNEQUIP_ARMOR");
    private static final List<Integer> REPLAY_CASE_IDS = replayCaseIds(cases());

    private Step1618RaidValidationStudy() {
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
        if (!canonicalTree(PveAdaptationStudy.JSON.readTree(protocolPath.toFile()))
            .equals(canonicalTree(protocol))) {
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
        int seedGroup = 0;
        for (final var raid : RAIDS) {
            final var mainSeeds = new TreeMap<String, Integer>();
            final var sizes = raid == RaidType.WOLFPACK ? List.of(1, 2, 3, 7, 13, 14, 15, 30)
                : List.of(1, 2, 3, 7, 10, 11, 13, 14, 15, 30);
            for (final var tier : TIERS) {
                for (final int size : sizes) {
                    for (final int level : LEVELS) {
                        final var cell = cell(raid, "MAIN", tier, size, level, false);
                        mainSeeds.put(context(tier, size, level), ++seedGroup);
                        add(result, "A_MAIN", "BASE", "", seedGroup, 10, cell, PveAdaptationStudy.inputFor(cell));
                    }
                }
            }
            for (final var attack : List.of(AttackType.SLASH, AttackType.PIERCE)) {
                for (final var armor : List.of("LEATHER", "CLOTH")) {
                    for (final var tier : TIERS) {
                        for (final int size : List.of(1, 3, 15)) {
                            for (final int level : List.of(1, 25, 50)) {
                                final var style = attack + "_" + armor;
                                final var cell = cell(raid, style, tier, size, level, false);
                                add(result, "B_TYPES", "BASE", "EXPLICIT_COMMON_TYPED_CONTROL", ++seedGroup, 10,
                                    cell, typedMembers(attack, armor, tier, size));
                            }
                        }
                    }
                }
            }
            for (final var tier : TIERS) {
                for (final int size : List.of(3, 15)) {
                    for (final int level : List.of(1, 25)) {
                        final var cell = cell(raid, "MAIN", tier, size, level, true);
                        add(result, "D_EFFECTS", "EFFECTS", "ATK_PLUS_20_PERCENT_HP_PLUS_15_PERCENT",
                            mainSeeds.get(context(tier, size, level)), 10, cell, PveAdaptationStudy.inputFor(cell));
                    }
                }
            }
            final var weak = weakWithDefaults();
            for (final var tier : TIERS) {
                for (final int size : List.of(3, 13)) {
                    for (final int level : List.of(10, 25)) {
                        ++seedGroup;
                        final var base = PveAdaptationStudy.inputFor(cell(raid, "MAIN", tier, size, level, false));
                        final var normal = PveAdaptationStudy.inputFor(cell(raid, "MAIN", tier, size + 1, level, false))
                            .get(size);
                        for (final var variant : VARIANTS) {
                            final var changed = variant(base, normal, weak, variant);
                            add(result, "E_CAUSAL", variant, changed.change(), seedGroup, 5,
                                cell(raid, "CAUSAL_" + variant, tier, changed.members().size(), level, false),
                                changed.members());
                        }
                    }
                }
            }
            for (final int size : List.of(1, 3, 13, 14, 30)) {
                for (final int level : LEVELS) {
                    final var members = new ArrayList<InputMember>();
                    for (int index = 0; index < size; index++) {
                        members.add(new InputMember("FULL_DEFAULTS", Position.FRONT, TargetingTactic.THREAT, defaults()));
                    }
                    add(result, "F_DEFAULTS", "BASE", "ALL_SEVEN_ACTUAL_DEFAULT_ITEMS", ++seedGroup, 8,
                        cell(raid, "FULL_DEFAULTS", Tier.BEGINNER, size, level, false), members);
                }
            }
            for (final var style : List.of("RITUAL", "COMMAND_CUIRASS")) {
                for (final var tier : TIERS) {
                    for (final int size : List.of(1, 3)) {
                        for (final int level : List.of(1, 25)) {
                            final var cell = cell(raid, style, tier, size, level, false);
                            add(result, "G_WORKABLE", "BASE", "ADDITIONAL_EXISTING_BUILD_DIRECTION",
                                ++seedGroup, 10, cell, PveAdaptationStudy.inputFor(cell));
                        }
                    }
                }
            }
        }
        if (result.size() != 1044 || battleCount(result) != 18480 || seedGroup != 828) {
            throw new IllegalStateException("The fixed steps 16-18 matrix changed");
        }
        return List.copyOf(result);
    }

    static List<InputMember> typedMembers(AttackType attack, String armor, Tier tier, int size) {
        final var build = Step12RepresentativeBuilds.hand(attack + "_" + armor,
            attack == AttackType.SLASH ? List.of("sword", "shortsword") : List.of("spear", "dirk"),
            armor, Position.FRONT, TargetingTactic.THREAT);
        final var gear = new Gear(build, EquipmentCatalogVersion.SCALING_V5);
        final var result = new ArrayList<InputMember>();
        for (int index = 0; index < size; index++) {
            result.add(new InputMember(build.code(), build.position(), build.tactic(), gear.items(tier.level())));
        }
        return List.copyOf(result);
    }

    static List<Integer> replayCaseIds(List<Sample> cases) {
        final var result = new ArrayList<Integer>();
        for (final var raid : RAIDS) {
            for (final int size : List.of(1, 3, 10, 11, 13, 14, 30)) {
                cases.stream().filter(sample -> sample.cell().encounter().equals(raid.name())
                    && sample.family().equals("A_MAIN") && sample.cell().tier() == Tier.MID
                    && sample.cell().raidLevel() == 25 && sample.cell().size() == size)
                    .findFirst().ifPresent(sample -> result.add(sample.caseId()));
            }
            for (final var family : List.of("B_TYPES", "D_EFFECTS", "E_CAUSAL", "F_DEFAULTS", "G_WORKABLE")) {
                result.add(cases.stream().filter(sample -> sample.cell().encounter().equals(raid.name())
                    && sample.family().equals(family)).findFirst().orElseThrow().caseId());
            }
        }
        if (result.size() != 34 || result.stream().distinct().count() != 34) {
            throw new IllegalStateException("The fixed replay selection changed");
        }
        return List.copyOf(result);
    }

    private static Map<String, Object> protocol(List<Sample> cases) throws Exception {
        final var result = new LinkedHashMap<String, Object>();
        result.put("protocolVersion", "STEP_16_18_FIXED_2026_11_15_V1");
        result.put("metric", METRIC);
        result.put("roundLimit", 2000);
        result.put("skillEventPolicy", "Counts log events, not unique activations; source side plus separate event kind");
        result.put("canonicalizationPolicy", "Only slots and availableOnSlots arrays are semantic sets");
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
        final var skillEvents = new TreeMap<String, Long>();
        final var skillEventsByKind = new TreeMap<String, Long>();
        int defeatedBossBattles = 0;
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
                final var row = runWithMembers(sample.cell(), sample.members(), generationSeed,
                    battleSeed(sample, pair), reverse, pair == 0 && REPLAY_CASE_IDS.contains(sample.caseId()));
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
                raw.write(PveAdaptationStudy.JSON.writeValueAsString(canonicalTree(row)));
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
                    mergeCounts(skillEvents, (Map<?, ?>) row.get("skillEvents"));
                    mergeCounts(skillEventsByKind, (Map<?, ?>) row.get("skillEventsByKind"));
                    if (Boolean.TRUE.equals(row.get("enemyBossDefeated"))) {
                        ++defeatedBossBattles;
                    }
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
        result.put("skillEvents", skillEvents);
        result.put("skillEventsByKind", skillEventsByKind);
        result.put("enemyBossDefeatedBattles", defeatedBossBattles);
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

    private static Cell cell(RaidType raid, String style, Tier tier, int size, int level, boolean effects) {
        return new Cell(Mode.RAID, raid.name(), style, tier, size, level, effects);
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
        if (!canonicalTree(first).equals(canonicalTree(second))) {
            throw new IllegalStateException("Different " + label);
        }
    }

    private static void mergeCounts(Map<String, Long> destination, Map<?, ?> source) {
        source.forEach((key, value) -> destination.merge((String) key, ((Number) value).longValue(), Long::sum));
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

    static JsonNode canonicalTree(Object value) {
        final JsonNode node = PveAdaptationStudy.JSON.valueToTree(value);
        canonicalize(node);
        return node;
    }

    private static void canonicalize(JsonNode node) {
        if (node instanceof ObjectNode object) {
            final var names = new ArrayList<String>();
            object.fieldNames().forEachRemaining(names::add);
            for (final var name : names) {
                final var child = object.get(name);
                final boolean itemSlots = name.equals("slots") && object.has("attacks") && object.has("defense")
                    && object.has("health") && object.has("critChance") && object.has("speed")
                    && object.has("progressionVersion");
                final boolean modifierSlots = name.equals("availableOnSlots") && object.has("activeEnum")
                    && object.has("type") && object.has("code") && object.has("locales");
                if ((itemSlots || modifierSlots) && child instanceof ArrayNode array) {
                    final var entries = new ArrayList<JsonNode>();
                    array.forEach(entries::add);
                    if (entries.stream().anyMatch(entry -> !entry.isTextual())) {
                        throw new IllegalStateException("Only textual equipment slot sets can be canonicalized");
                    }
                    entries.sort(java.util.Comparator.comparing(JsonNode::asText));
                    array.removeAll();
                    entries.forEach(array::add);
                } else {
                    canonicalize(child);
                }
            }
        } else if (node instanceof ArrayNode array) {
            array.forEach(Step1618RaidValidationStudy::canonicalize);
        }
    }

    private static Map<String, Object> runWithMembers(
        Cell cell, List<InputMember> members, long generationSeed, long battleSeed, boolean reverse, boolean keepLogs
    ) throws Exception {
        final var prepared = RandomUtils.withSeed(generationSeed, () -> PveAdaptationStudy.prepareMembers(cell, members));
        final var players = prepared.players();
        final var enemies = prepared.generated().enemies();
        final var row = new LinkedHashMap<String, Object>();
        row.put("cell", cell);
        row.put("generationSeed", generationSeed);
        row.put("battleSeed", battleSeed);
        row.put("reverse", reverse);
        row.put("catalog", EquipmentCatalogVersion.SCALING_V5);
        row.put("playerSkills", PveAdaptationStudy.FORMULA);
        row.put("npcSkills", PveAdaptationStudy.FORMULA);
        row.put("tempo", PveAdaptationStudy.TEMPO);
        row.put("roundLimit", 2000);
        row.put("members", prepared.members());
        row.put("sourceEffects", combatEffects(cell).effects());
        row.put("combatEffects", combatEffects(cell).effects());
        row.put("calibration", prepared.generated().calibration());
        row.put("calibrationMetric", prepared.generated().diagnostics().orElseThrow().assessmentVersion());
        row.put("generationDiagnostics", prepared.generated().diagnostics());
        row.put("playerPower", players.stream().mapToDouble(BattlePersonage::legacyPower).sum());
        row.put("enemyPower", enemies.stream().mapToDouble(BattlePersonage::legacyPower).sum());
        row.put("playerBasis", basis(players));
        row.put("enemyBasis", basis(enemies));
        row.put("hasEnemyBoss", !cell.encounter().equals(RaidType.MAGGEESE_FLOCK.name()));
        try {
            final var result = RandomUtils.withSeed(battleSeed, () -> new Battle(battleSeed).process(
                reverse ? players : enemies, reverse ? enemies : players, 2000, PveAdaptationStudy.TEMPO));
            final long alivePlayers = players.stream().filter(BattlePersonage::isAlive).count();
            final long aliveEnemies = enemies.stream().filter(BattlePersonage::isAlive).count();
            row.put("outcome", alivePlayers > 0 ? "WIN" : aliveEnemies > 0 ? "LOSS" : "DRAW");
            row.put("rounds", result.rounds());
            row.put("actualEnemyPositions", enemies.stream().map(enemy ->
                result.initState().lines().get(result.initState().personagesById().get(enemy.id()).lineIndex())
                    .position()).toList());
            row.put("initState", result.initState());
            row.put("actionCount", result.actionLog().events().size());
            row.put("normalAttempts", result.actionLog().traceEvents().stream()
                .filter(BattleTraceEvent.NormalAttackAttempt.class::isInstance).count());
            row.put("moves", result.actionLog().events().stream()
                .filter(BattleEvent.MovedTowardEnemy.class::isInstance).count());
            row.put("forcedMoves", result.actionLog().events().stream()
                .filter(BattleEvent.PersonageForcedMove.class::isInstance).count());
            row.put("actionSha256", digest(result.actionLog()));
            row.put("traceSha256", digest(result.actionLog().traceEvents()));
            final var skillCounters = skillCounters(result.actionLog().events(), players, enemies);
            row.put("skillEvents", skillCounters.events());
            row.put("skillEventsByKind", skillCounters.byKind());
            if (keepLogs) {
                row.put("actionLog", result.actionLog());
                row.put("traceEvents", result.actionLog().traceEvents());
            }
        } catch (IllegalStateException failure) {
            if (failure.getMessage() == null || !failure.getMessage().startsWith("Battle did not finish within")) {
                throw failure;
            }
            row.put("outcome", "TIMEOUT");
            row.put("rounds", 2000);
        }
        row.put("enemyBossDefeated", !cell.encounter().equals(RaidType.MAGGEESE_FLOCK.name())
            && !enemies.getFirst().isAlive());
        row.put("playerTotals", totals(players));
        row.put("enemyTotals", totals(enemies));
        return row;
    }

    private static SkillCounters skillCounters(
        List<BattleEvent> events, List<BattlePersonage> players, List<BattlePersonage> enemies
    ) {
        final var skills = List.of(ActiveEnum.THORNS, ActiveEnum.SELF_HEAL, ActiveEnum.BLEEDING,
            ActiveEnum.BERSERK, ActiveEnum.DOUBLE_ATTACK, ActiveEnum.HIT_AND_RUN);
        final var playerIds = players.stream().map(BattlePersonage::id).collect(java.util.stream.Collectors.toSet());
        final var enemyIds = enemies.stream().map(BattlePersonage::id).collect(java.util.stream.Collectors.toSet());
        final var counts = new TreeMap<String, Long>();
        final var byKind = new TreeMap<String, Long>();
        for (final var side : List.of("PLAYER", "ENEMY")) {
            skills.forEach(skill -> counts.put(side + ":" + skill, 0L));
        }
        for (final var event : events) {
            final var observation = observation(event);
            if (observation == null || !skills.contains(observation.skill())) {
                continue;
            }
            final String side;
            if (playerIds.contains(observation.source())) {
                side = "PLAYER";
            } else if (enemyIds.contains(observation.source())) {
                side = "ENEMY";
            } else {
                throw new IllegalStateException("Skill event has an unknown source");
            }
            final var key = side + ":" + observation.skill();
            counts.merge(key, 1L, Long::sum);
            byKind.merge(key + ":" + observation.kind(), 1L, Long::sum);
        }
        return new SkillCounters(counts, byKind);
    }

    private static SkillObservation observation(BattleEvent event) {
        return switch (event) {
            case BattleEvent.SkillWindowUsed used ->
                new SkillObservation(used.personageId(), used.skill(), "WINDOW");
            case BattleEvent.ScalingSkillDamage damage ->
                new SkillObservation(damage.sourceId(), damage.skill(), damage.periodic()
                    ? "SCALING_PERIODIC_DAMAGE" : "SCALING_DIRECT_DAMAGE");
            case BattleEvent.SkillDamage damage ->
                new SkillObservation(damage.sourceId(), damage.skill(), "LEGACY_DIRECT_DAMAGE");
            case BattleEvent.EffectDamage damage ->
                new SkillObservation(damage.sourceId(), damage.skill(), "LEGACY_PERIODIC_DAMAGE");
            case BattleEvent.PersonageHealed heal ->
                new SkillObservation(heal.personageId(), heal.skill(), "HEAL");
            case BattleEvent.PersonageAttackBuffed buff ->
                new SkillObservation(buff.personageId(), buff.skill(), "BUFF");
            case BattleEvent.PersonageForcedMove move ->
                new SkillObservation(move.sourceId(), move.skill(), "FORCED_MOVE");
            default -> null;
        };
    }

    private static ru.homyakin.seeker.game.personage.models.effect.PersonageEffects combatEffects(Cell cell) {
        return cell.effects() ? new ru.homyakin.seeker.game.personage.models.effect.PersonageEffects(Map.of(
            ru.homyakin.seeker.game.personage.models.effect.PersonageEffectType.MENU_ITEM_EFFECT,
            new ru.homyakin.seeker.game.personage.models.effect.PersonageEffect(
                new ru.homyakin.seeker.game.effect.Effect.Multiplier(20,
                    ru.homyakin.seeker.game.effect.EffectCharacteristic.ATTACK), null),
            ru.homyakin.seeker.game.personage.models.effect.PersonageEffectType.WORKER_OF_DAY_EFFECT,
            new ru.homyakin.seeker.game.personage.models.effect.PersonageEffect(
                new ru.homyakin.seeker.game.effect.Effect.Multiplier(15,
                    ru.homyakin.seeker.game.effect.EffectCharacteristic.HEALTH), null)
        )) : ru.homyakin.seeker.game.personage.models.effect.PersonageEffects.EMPTY;
    }

    private static List<Map<String, Object>> basis(List<BattlePersonage> team) {
        return team.stream().map(personage -> Map.<String, Object>of(
            "id", personage.id(), "health", personage.health(), "attack", personage.battlePersonageStats().attack(),
            "defense", personage.battlePersonageStats().defense(), "range", personage.range(),
            "declaredPosition", personage.startPosition(), "attackTypes", personage.attackTypes().stream().sorted().toList(),
            "skills", personage.skillSnapshots())).toList();
    }

    private static Map<String, Long> totals(List<BattlePersonage> team) {
        final var stats = team.stream().map(BattlePersonage::battlePersonageStats).toList();
        return Map.of("survivors", team.stream().filter(BattlePersonage::isAlive).count(),
            "initialHealth", stats.stream().mapToLong(BattlePersonageStats::initialHealth).sum(),
            "remainHealth", stats.stream().mapToLong(BattlePersonageStats::remainHealth).sum(),
            "normalAttacksHit", stats.stream().mapToLong(BattlePersonageStats::normalAttackCount).sum(),
            "normalDamage", stats.stream().mapToLong(BattlePersonageStats::normalDamageDealt).sum(),
            "critDamage", stats.stream().mapToLong(BattlePersonageStats::critDamageDealt).sum(),
            "skillDamage", stats.stream().mapToLong(BattlePersonageStats::skillDamageDealt).sum(),
            "turns", stats.stream().mapToLong(BattlePersonageStats::turnsCount).sum());
    }

    private static String digest(Object value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(
            PveAdaptationStudy.JSON.writeValueAsBytes(canonicalTree(value))));
    }

    private record SkillObservation(java.util.UUID source, ActiveEnum skill, String kind) { }

    private record SkillCounters(Map<String, Long> events, Map<String, Long> byKind) { }

    private static Map<String, String> sourceHashes() throws Exception {
        final var paths = new ArrayList<String>();
        for (final var line : Files.readAllLines(Path.of("documentation/raid-redesign-step-15-6-evidence/inputs.sha256"))) {
            if (!line.isBlank()) {
                paths.add(line.substring(66));
            }
        }
        paths.addAll(List.of(
            "game/src/main/java/ru/homyakin/seeker/game/event/pve/PveEquipmentPowerSum.java",
            "game/src/test/java/ru/homyakin/seeker/game/event/pve/PveEquipmentPowerSumTest.java",
            "game/src/test/java/ru/homyakin/seeker/game/event/raid/generator/WolfEquipmentScalingTest.java",
            "game/src/test/java/ru/homyakin/seeker/game/event/raid/generator/ZombieEquipmentScalingTest.java",
            "game/src/test/java/ru/homyakin/seeker/game/event/raid/generator/MaggeeseEquipmentScalingTest.java",
            "game/src/test/java/ru/homyakin/seeker/game/battle/RaidHistoricalCompatibilityStudy.java",
            "game/src/test/java/ru/homyakin/seeker/game/battle/Step1618RaidValidationStudy.java",
            "game/src/test/java/ru/homyakin/seeker/game/battle/Step1618RaidValidationStudyTest.java",
            "documentation/raid-redesign-steps-16-18-evidence/protocol.md"
        ));
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
        Files.writeString(path, PveAdaptationStudy.JSON.writerWithDefaultPrettyPrinter().writeValueAsString(canonicalTree(value)),
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
