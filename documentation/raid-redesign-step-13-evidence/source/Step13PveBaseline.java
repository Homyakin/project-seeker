package ru.homyakin.seeker.game.battle;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.GZIPOutputStream;
import ru.homyakin.seeker.game.battle.Step123BattleRunner.Gear;
import ru.homyakin.seeker.game.battle.Step123ReferenceTeams.Specialization;
import ru.homyakin.seeker.game.battle.Step123ReferenceTeams.Tier;
import ru.homyakin.seeker.game.battle.Step12RepresentativeBuilds.Assignment;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;
import ru.homyakin.seeker.game.battle.targeting.TargetingTactic;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyPveTemplate;
import ru.homyakin.seeker.game.event.anomaly.generator.AnomalySafePveGenerator;
import ru.homyakin.seeker.game.event.launched.RaidParams;
import ru.homyakin.seeker.game.event.models.EventStatus;
import ru.homyakin.seeker.game.event.raid.generator.RaidGenerator;
import ru.homyakin.seeker.game.event.raid.models.LaunchedRaidEvent;
import ru.homyakin.seeker.game.event.raid.models.RaidType;
import ru.homyakin.seeker.game.event.raid.processing.RaidRewardGenerator;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogVersion;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffects;
import ru.homyakin.seeker.utils.RandomUtils;

/** Diagnostic only: actual unchanged generators and Battle.process, no engine overlays or DB. */
public final class Step13PveBaseline {
    private static final JsonMapper JSON = JsonMapper.builder().addModule(new Jdk8Module()).build();
    private static final List<Tier> TIERS = List.of(Tier.BEGINNER, Tier.MID, Tier.STRONG);
    private static final int ROUND_LIMIT = 2000;
    private static final long GENERATION_ROOT = 2_026_110_713_000L;
    private static final long BATTLE_ROOT = 3_026_110_713_000L;

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("One new output directory required");
        final var output = Path.of(args[0]);
        Files.createDirectory(output);
        final int pairs = Integer.getInteger("baseline.pairs", 20);
        if (pairs < 1 || pairs > 100) throw new IllegalArgumentException("Pairs must be in 1..100");
        final var cells = cells();
        final var summaries = new ArrayList<Map<String, Object>>();
        int caseIndex = 0;
        try (var definitions = writer(output.resolve("cases.jsonl.gz"));
             var raw = writer(output.resolve("battles.jsonl.gz"))) {
            for (final var cell : cells.values()) {
                ++caseIndex;
                final var gear = gear(cell.style(), cell.tier(), cell.size());
                definitions.write(JSON.writeValueAsString(Map.of(
                    "caseId", caseIndex, "cell", cell,
                    "members", gear.stream().map(member -> Map.of(
                        "build", member.build().code(), "position", member.build().position(),
                        "tactic", member.build().tactic(), "catalog", member.catalog(),
                        "items", items(member, cell)
                    )).toList()
                )));
                definitions.newLine();
                int wins = 0, losses = 0, draws = 0, timeouts = 0, maxRounds = 0;
                long roundsSum = 0;
                double enemyPowerRatio = 0;
                final var enemyCounts = new java.util.TreeSet<Integer>();
                final var pairScores = new ArrayList<Double>();
                for (int pair = 0; pair < pairs; pair++) {
                    double score = 0;
                    for (boolean reverse : List.of(false, true)) {
                        final long generationSeed = GENERATION_ROOT + caseIndex * 1000L + pair;
                        final long battleSeed = BATTLE_ROOT + caseIndex * 1000L + pair;
                        final var prepared = RandomUtils.withSeed(generationSeed, () -> prepare(cell, gear));
                        final var players = prepared.players();
                        final var enemies = prepared.enemies();
                        final var before = enemies.stream().map(personage -> Map.of(
                            "health", personage.health(), "attack", personage.battlePersonageStats().attack(),
                            "defense", personage.battlePersonageStats().defense(), "range", personage.range(),
                            "declaredPosition", personage.startPosition(), "attackTypes", personage.attackTypes(),
                            "skills", personage.skillSnapshots()
                        )).toList();
                        final double playerPower = players.stream().mapToDouble(BattlePersonage::legacyPower).sum();
                        final double enemyPower = enemies.stream().mapToDouble(BattlePersonage::legacyPower).sum();
                        BattleResult result = null;
                        String outcome;
                        int rounds;
                        try {
                            result = RandomUtils.withSeed(battleSeed, () -> new Battle(battleSeed).process(
                                reverse ? players : enemies, reverse ? enemies : players,
                                ROUND_LIMIT, BattleTempoVersion.ACCELERATED_V1
                            ));
                            rounds = result.rounds();
                            final long alivePlayers = players.stream().filter(BattlePersonage::isAlive).count();
                            final long aliveEnemies = enemies.stream().filter(BattlePersonage::isAlive).count();
                            outcome = alivePlayers > 0 ? "WIN" : aliveEnemies > 0 ? "LOSS" : "DRAW";
                        } catch (IllegalStateException failure) {
                            if (!failure.getMessage().startsWith("Battle did not finish within")) throw failure;
                            outcome = "TIMEOUT";
                            rounds = ROUND_LIMIT;
                        }
                        for (final var personage : players) {
                            if (!personage.usesScalingCombatRules()) throw new IllegalStateException("Not V3 combat");
                            for (final var skill : personage.skillSnapshots()) {
                                if (skill.formulaVersion() != SkillFormulaVersion.SCALING_SKILLS_V3)
                                    throw new IllegalStateException("Not V3 skill");
                            }
                        }
                        for (final var enemy : enemies) {
                            if (enemy.skillSnapshots().stream().anyMatch(skill ->
                                skill.formulaVersion() != SkillFormulaVersion.LEGACY_SKILLS_V1))
                                throw new IllegalStateException("Unexpected NPC formula");
                        }
                        for (final var personage : java.util.stream.Stream.concat(players.stream(), enemies.stream()).toList()) {
                            if (personage.battleTempoVersion() != BattleTempoVersion.ACCELERATED_V1)
                                throw new IllegalStateException("Wrong tempo");
                        }
                        switch (outcome) {
                            case "WIN" -> { ++wins; score += 1; }
                            case "LOSS" -> ++losses;
                            case "DRAW" -> { ++draws; score += 0.5; }
                            case "TIMEOUT" -> ++timeouts;
                            default -> throw new IllegalStateException(outcome);
                        }
                        roundsSum += rounds;
                        maxRounds = Math.max(maxRounds, rounds);
                        enemyPowerRatio += enemyPower / playerPower;
                        enemyCounts.add(enemies.size());
                        final var row = new LinkedHashMap<String, Object>();
                        row.put("caseId", caseIndex); row.put("pair", pair); row.put("reverse", reverse);
                        row.put("generationSeed", generationSeed); row.put("battleSeed", battleSeed);
                        row.put("outcome", outcome); row.put("rounds", rounds);
                        row.put("playerPower", playerPower); row.put("enemyPower", enemyPower);
                        row.put("enemyBasis", before);
                        row.put("playerTotals", totals(players)); row.put("enemyTotals", totals(enemies));
                        final boolean playerVictory = outcome.equals("WIN");
                        row.put("raidGoldWithoutBonuses", cell.mode().equals("RAID")
                            && (outcome.equals("WIN") || outcome.equals("LOSS"))
                            ? players.stream().mapToInt(player -> new RaidRewardGenerator().calculateReward(
                                playerVictory, new ru.homyakin.seeker.game.battle.result.PersonageBattleResult(
                                    null, player.battlePersonageStats()), false, 0)).sum() : null);
                        if (result != null) {
                            final var captured = result;
                            row.put("actualEnemyPositions", enemies.stream().map(enemy ->
                                captured.initState().lines().get(captured.initState().personagesById()
                                    .get(enemy.id()).lineIndex()).position()).toList());
                        }
                        raw.write(JSON.writeValueAsString(row)); raw.newLine();
                    }
                    pairScores.add(score / 2);
                }
                final var summary = new LinkedHashMap<String, Object>();
                summary.put("caseId", caseIndex); summary.put("cell", cell);
                summary.put("battles", 2 * pairs); summary.put("wins", wins); summary.put("losses", losses);
                summary.put("draws", draws); summary.put("timeouts", timeouts);
                summary.put("pairScores", pairScores);
                summary.put("meanRounds", (double) roundsSum / (2 * pairs)); summary.put("maxRounds", maxRounds);
                summary.put("meanEnemyPowerRatio", enemyPowerRatio / (2 * pairs)); summary.put("enemyCounts", enemyCounts);
                summaries.add(summary);
                if (caseIndex % 100 == 0) System.out.println("completed cells=" + caseIndex);
            }
        }
        Files.writeString(output.resolve("summary.json"), JSON.writerWithDefaultPrettyPrinter().writeValueAsString(Map.of(
            "purpose", "PRE_ADAPTATION_DIAGNOSTIC_NOT_ACCEPTANCE", "cases", summaries,
            "pairs", pairs, "generationRoot", GENERATION_ROOT, "battleRoot", BATTLE_ROOT,
            "roundLimit", ROUND_LIMIT, "catalog", "SCALING_V5", "playerSkills", "SCALING_SKILLS_V3",
            "npcSkills", "LEGACY_SKILLS_V1", "tempo", "ACCELERATED_V1"
        )), StandardOpenOption.CREATE_NEW);
        System.out.println("complete cells=" + caseIndex + " battles=" + caseIndex * pairs * 2);
    }

    private static LinkedHashMap<String, Cell> cells() {
        final var cells = new LinkedHashMap<String, Cell>();
        for (final var type : RaidType.values()) {
            for (final var tier : TIERS) {
                for (final int size : List.of(1, 2, 3, 7, 8, 10, 11, 13, 14, 15, 30)) {
                    for (final int level : List.of(1, 10, 25, 50)) add(cells, new Cell("RAID", type.name(), "MAIN", tier, size, level));
                }
                for (final var specialization : Specialization.values()) {
                    for (final int level : List.of(1, 10, 50)) add(cells, new Cell("RAID", type.name(), specialization.name(), tier, 3, level));
                }
                for (final var style : List.of("COMMAND_CUIRASS", "RITUAL")) {
                    for (final int size : List.of(1, 3, 7, 15)) {
                        for (final int level : List.of(1, 10, 50)) add(cells, new Cell("RAID", type.name(), style, tier, size, level));
                    }
                }
            }
            for (int size = 1; size <= 30; size++) add(cells, new Cell("RAID", type.name(), "MAIN", Tier.MID, size, 10));
        }
        for (final var template : AnomalyPveTemplate.values()) {
            for (final var tier : TIERS) {
                for (final var style : List.of("MAIN", "COMMAND_CUIRASS", "RITUAL")) {
                    for (int size = 1; size <= 5; size++) add(cells, new Cell("ANOMALY", template.name(), style, tier, size, 0));
                }
                for (final var specialization : Specialization.values()) add(cells, new Cell("ANOMALY", template.name(), specialization.name(), tier, 3, 0));
            }
        }
        return cells;
    }

    private static void add(Map<String, Cell> result, Cell cell) { result.put(cell.toString(), cell); }

    private static List<Gear> gear(String style, Tier tier, int size) {
        if (style.equals("MAIN")) return Step123ReferenceTeams.mainTeam(tier, size).members();
        if (!List.of("COMMAND_CUIRASS", "RITUAL").contains(style))
            return Step123ReferenceTeams.specializedTeam(tier, Specialization.valueOf(style)).members();
        var build = style.equals("COMMAND_CUIRASS")
            ? Step12RepresentativeBuilds.hand(style, List.of("command_staff"), "LEATHER", Position.FRONT,
                TargetingTactic.WOUNDED_HUNTER).replaceItem("breastplate", "cuirass")
            : Step12RepresentativeBuilds.hand(style, List.of("ritual_staff"), "NEW_ARCANE", Position.BACK,
                TargetingTactic.EXECUTIONER);
        if (tier.profilePoints() > 0) {
            build = build.withAssignments(Map.of(
                style.equals("COMMAND_CUIRASS") ? "command_staff" : "ritual_staff",
                new Assignment(style.equals("COMMAND_CUIRASS") ? ActiveEnum.BLEEDING : ActiveEnum.ACCUMULATION,
                    tier.profilePoints() == 2 ? ItemRarity.UNCOMMON : ItemRarity.RARE),
                style.equals("COMMAND_CUIRASS") ? "cuirass" : "phantom_vestment",
                new Assignment(ActiveEnum.SELF_HEAL, tier.profilePoints() == 2 ? ItemRarity.RARE : ItemRarity.LEGENDARY)
            ));
        }
        final var result = new ArrayList<Gear>();
        for (int index = 0; index < size; index++) {
            result.add(style.equals("RITUAL") && size > 1 && index == 0
                ? Step123ReferenceTeams.mainTeam(tier, 2).members().getFirst()
                : new Gear(build, EquipmentCatalogVersion.SCALING_V5));
        }
        return result;
    }

    private static List<Item> items(Gear member, Cell cell) {
        final var original = member.items(cell.tier().level());
        return cell.mode().equals("ANOMALY") ? original.stream().map(Item::withoutStormEnhance).toList() : original;
    }

    private static Prepared prepare(Cell cell, List<Gear> gear) {
        final var players = gear.stream().map(member -> {
            final var personage = BattlePersonage.forCombat(items(member, cell), member.build().position(),
                PersonageEffects.EMPTY, Optional.of(member.build().code()), SkillFormulaVersion.SCALING_SKILLS_V3);
            personage.setTargetingTactic(member.build().tactic());
            return personage;
        }).toList();
        final var enemies = cell.mode().equals("RAID")
            ? new RaidGenerator().generate(RaidType.valueOf(cell.encounter()), new LaunchedRaidEvent(
                1, 1, LocalDateTime.of(2026, 11, 7, 0, 0), LocalDateTime.of(2026, 11, 7, 1, 0),
                EventStatus.LAUNCHED, new RaidParams(cell.raidLevel(), 0)), players)
            : new AnomalySafePveGenerator().generate(AnomalyPveTemplate.valueOf(cell.encounter()), players);
        return new Prepared(players, enemies);
    }

    private static Map<String, Long> totals(List<BattlePersonage> team) {
        final var stats = team.stream().map(BattlePersonage::battlePersonageStats).toList();
        final var result = new LinkedHashMap<String, Long>();
        result.put("survivors", team.stream().filter(BattlePersonage::isAlive).count());
        result.put("initialHealth", stats.stream().mapToLong(BattlePersonageStats::initialHealth).sum());
        result.put("remainHealth", stats.stream().mapToLong(BattlePersonageStats::remainHealth).sum());
        result.put("normalAttacks", stats.stream().mapToLong(BattlePersonageStats::normalAttackCount).sum());
        result.put("normalDamage", stats.stream().mapToLong(BattlePersonageStats::normalDamageDealt).sum());
        result.put("critDamage", stats.stream().mapToLong(BattlePersonageStats::critDamageDealt).sum());
        result.put("skillDamage", stats.stream().mapToLong(BattlePersonageStats::skillDamageDealt).sum());
        result.put("skillDamageCount", stats.stream().mapToLong(BattlePersonageStats::skillDamageCount).sum());
        result.put("turns", stats.stream().mapToLong(BattlePersonageStats::turnsCount).sum());
        return result;
    }

    private static BufferedWriter writer(Path path) throws Exception {
        return new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(
            path, StandardOpenOption.CREATE_NEW)), StandardCharsets.UTF_8));
    }

    private record Cell(String mode, String encounter, String style, Tier tier, int size, int raidLevel) { }
    private record Prepared(List<BattlePersonage> players, List<BattlePersonage> enemies) { }
}
