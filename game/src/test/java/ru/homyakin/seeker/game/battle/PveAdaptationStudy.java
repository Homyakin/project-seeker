package ru.homyakin.seeker.game.battle;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.GZIPOutputStream;
import ru.homyakin.seeker.game.battle.Step123ReferenceTeams.Specialization;
import ru.homyakin.seeker.game.battle.Step123ReferenceTeams.Tier;
import ru.homyakin.seeker.game.battle.Step123BattleRunner.Gear;
import ru.homyakin.seeker.game.battle.Step12RepresentativeBuilds.Assignment;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;
import ru.homyakin.seeker.game.battle.targeting.TargetingTactic;
import ru.homyakin.seeker.game.effect.Effect;
import ru.homyakin.seeker.game.effect.EffectCharacteristic;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyPveTemplate;
import ru.homyakin.seeker.game.event.anomaly.generator.AnomalySafePveGenerator;
import ru.homyakin.seeker.game.event.launched.RaidParams;
import ru.homyakin.seeker.game.event.models.EventStatus;
import ru.homyakin.seeker.game.event.pve.GeneratedPveEnemies;
import ru.homyakin.seeker.game.event.raid.generator.RaidGenerator;
import ru.homyakin.seeker.game.event.raid.models.LaunchedRaidEvent;
import ru.homyakin.seeker.game.event.raid.models.RaidType;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogVersion;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffect;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffectType;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffects;
import ru.homyakin.seeker.utils.RandomUtils;

/** Actual generators and Battle.process, with explicit inputs and no application/DB startup. */
public final class PveAdaptationStudy {
    static final JsonMapper JSON = JsonMapper.builder().addModule(new Jdk8Module())
        .addModule(new JavaTimeModule()).enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS).build();
    static final SkillFormulaVersion FORMULA = SkillFormulaVersion.SCALING_SKILLS_V3;
    static final BattleTempoVersion TEMPO = BattleTempoVersion.ACCELERATED_V1;
    private static final int ROUND_LIMIT = 2000;
    private static final long GENERATION_ROOT = 2_026_110_714_000L;
    private static final long BATTLE_ROOT = 3_026_110_714_000L;

    private PveAdaptationStudy() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1 || args[0].isBlank()) {
            throw new IllegalArgumentException("Specify one new output directory");
        }
        final int pairs = Integer.getInteger("pve.study.pairs", 3);
        if (pairs < 1 || pairs > 100) {
            throw new IllegalArgumentException("Pairs must be in 1..100");
        }
        final var output = Path.of(args[0]);
        Files.createDirectory(output);
        final var cases = cases(Boolean.getBoolean("pve.study.boundaries"));
        final var summaries = new ArrayList<Map<String, Object>>();
        try (var raw = writer(output.resolve("battles.jsonl.gz"))) {
            int caseId = 0;
            for (final var cell : cases) {
                ++caseId;
                final var outcomes = new LinkedHashMap<String, Integer>();
                long rounds = 0;
                int maxRounds = 0;
                for (int pair = 0; pair < pairs; pair++) {
                    for (final boolean reverse : List.of(false, true)) {
                        final var row = run(cell, GENERATION_ROOT + caseId * 1000L + pair,
                            BATTLE_ROOT + caseId * 1000L + pair, reverse);
                        row.put("caseId", caseId);
                        row.put("pair", pair);
                        raw.write(JSON.writeValueAsString(row));
                        raw.newLine();
                        outcomes.merge((String) row.get("outcome"), 1, Integer::sum);
                        rounds += (int) row.get("rounds");
                        maxRounds = Math.max(maxRounds, (int) row.get("rounds"));
                    }
                }
                summaries.add(Map.of("caseId", caseId, "cell", cell, "outcomes", outcomes,
                    "meanRounds", (double) rounds / (2 * pairs), "maxRounds", maxRounds));
                if (caseId % 20 == 0) {
                    System.out.println("completed cells=" + caseId);
                }
            }
        }
        Files.writeString(output.resolve("summary.json"), JSON.writerWithDefaultPrettyPrinter().writeValueAsString(
            Map.of("purpose", "STRUCTURAL_CHECK_NOT_BALANCE_ACCEPTANCE", "cases", summaries,
                "pairs", pairs, "generationRoot", GENERATION_ROOT, "battleRoot", BATTLE_ROOT,
                "roundLimit", ROUND_LIMIT, "catalog", EquipmentCatalogVersion.SCALING_V5,
                "playerSkills", FORMULA, "npcSkills", FORMULA, "tempo", TEMPO)
        ), StandardOpenOption.CREATE_NEW);
        System.out.println("complete cells=" + cases.size() + " battles=" + cases.size() * pairs * 2);
    }

    static Map<String, Object> run(Cell cell, long generationSeed, long battleSeed, boolean reverse) throws Exception {
        return run(cell, gearFor(cell), generationSeed, battleSeed, reverse);
    }

    static Map<String, Object> run(
        Cell cell, List<Gear> gear, long generationSeed, long battleSeed, boolean reverse
    ) throws Exception {
        return runWithMembers(cell, inputFor(cell, gear), generationSeed, battleSeed, reverse);
    }

    static Map<String, Object> runWithMembers(
        Cell cell, List<InputMember> members, long generationSeed, long battleSeed, boolean reverse
    ) throws Exception {
        final var prepared = RandomUtils.withSeed(generationSeed, () -> prepareMembers(cell, members));
        final var players = prepared.players();
        final var enemies = prepared.generated().enemies();
        final var row = new LinkedHashMap<String, Object>();
        row.put("cell", cell);
        row.put("generationSeed", generationSeed);
        row.put("battleSeed", battleSeed);
        row.put("reverse", reverse);
        row.put("catalog", EquipmentCatalogVersion.SCALING_V5);
        row.put("playerSkills", FORMULA);
        row.put("npcSkills", FORMULA);
        row.put("tempo", TEMPO);
        row.put("roundLimit", ROUND_LIMIT);
        row.put("members", prepared.members());
        row.put("sourceEffects", sourceEffects(cell).effects());
        row.put("combatEffects", combatEffects(cell).effects());
        row.put("calibration", prepared.generated().calibration());
        row.put("calibrationMetric", prepared.generated().diagnostics()
            .map(diagnostics -> diagnostics.assessmentVersion()).orElse("LEGACY_SUM_V1"));
        row.put("generationDiagnostics", prepared.generated().diagnostics());
        row.put("playerPower", players.stream().mapToDouble(BattlePersonage::legacyPower).sum());
        row.put("enemyPower", enemies.stream().mapToDouble(BattlePersonage::legacyPower).sum());
        row.put("playerBasis", basis(players));
        row.put("enemyBasis", basis(enemies));
        try {
            final var result = RandomUtils.withSeed(battleSeed, () -> new Battle(battleSeed).process(
                reverse ? players : enemies, reverse ? enemies : players, ROUND_LIMIT, TEMPO));
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
        } catch (IllegalStateException failure) {
            if (failure.getMessage() == null || !failure.getMessage().startsWith("Battle did not finish within")) {
                throw failure;
            }
            row.put("outcome", "TIMEOUT");
            row.put("rounds", ROUND_LIMIT);
        }
        row.put("playerTotals", totals(players));
        row.put("enemyTotals", totals(enemies));
        return row;
    }

    static Prepared prepare(Cell cell) {
        return prepare(cell, gearFor(cell));
    }

    static Prepared prepare(Cell cell, List<Gear> gear) {
        return prepareMembers(cell, inputFor(cell, gear));
    }

    static Prepared prepareMembers(Cell cell, List<InputMember> supplied) {
        if (supplied.size() != cell.size()) {
            throw new IllegalArgumentException("The supplied gear must match the declared team size");
        }
        final var players = new ArrayList<BattlePersonage>();
        final var members = new ArrayList<Member>();
        for (final var member : supplied) {
            final var original = member.items();
            final var items = combatItems(cell, original);
            final var player = BattlePersonage.forCombat(items, member.position(), combatEffects(cell),
                Optional.of(member.build()), FORMULA);
            player.setTargetingTactic(member.tactic());
            players.add(player);
            members.add(new Member(member.build(), member.position(), member.tactic().name(),
                original, items));
        }
        final GeneratedPveEnemies generated;
        if (cell.mode() == Mode.RAID) {
            final var event = new LaunchedRaidEvent(1, 1, LocalDateTime.of(2026, 11, 7, 0, 0),
                LocalDateTime.of(2026, 11, 7, 1, 0), EventStatus.LAUNCHED, new RaidParams(cell.raidLevel(), 0));
            generated = new RaidGenerator().generateCalibrated(RaidType.valueOf(cell.encounter()), event, players, FORMULA);
        } else {
            generated = new AnomalySafePveGenerator().generateCalibrated(
                AnomalyPveTemplate.valueOf(cell.encounter()), players, FORMULA);
        }
        for (final var personage : java.util.stream.Stream.concat(players.stream(), generated.enemies().stream()).toList()) {
            if (!personage.usesScalingCombatRules() || personage.skillSnapshots().stream()
                .anyMatch(skill -> skill.formulaVersion() != FORMULA)) {
                throw new IllegalStateException("A trial participant is not V3");
            }
        }
        return new Prepared(List.copyOf(players), generated, List.copyOf(members));
    }

    static List<InputMember> inputFor(Cell cell) {
        return inputFor(cell, gearFor(cell));
    }

    private static List<InputMember> inputFor(Cell cell, List<Gear> gear) {
        return gear.stream().map(member -> new InputMember(member.build().code(), member.build().position(),
            member.build().tactic(), member.items(cell.tier().level()))).toList();
    }

    static List<Gear> gearFor(Cell cell) {
        if (cell.style().equals("MAIN")) {
            return Step123ReferenceTeams.mainTeam(cell.tier(), cell.size()).members();
        }
        if (List.of("PIERCE_ARCANE", "RAPIER_ARCANE").contains(cell.style())) {
            final var weapon = cell.style().equals("PIERCE_ARCANE") ? "spear" : "rapier";
            var counter = Step12RepresentativeBuilds.hand(cell.style(), List.of(weapon, "dirk"),
                "NEW_ARCANE", Position.FRONT, TargetingTactic.WOUNDED_HUNTER);
            if (cell.tier().profilePoints() > 0) {
                final var rarity = cell.tier().profilePoints() == 2 ? ItemRarity.UNCOMMON : ItemRarity.RARE;
                counter = counter.withAssignments(Map.of(weapon, new Assignment(ActiveEnum.BLEEDING, rarity),
                    "phantom_vestment", new Assignment(ActiveEnum.SELF_HEAL, rarity)));
            }
            final var result = new ArrayList<Gear>();
            for (int index = 0; index < cell.size(); index++) {
                result.add(cell.size() > 1 && index == 0
                    ? Step123ReferenceTeams.mainTeam(cell.tier(), 2).members().getFirst()
                    : new Gear(counter, EquipmentCatalogVersion.SCALING_V5));
            }
            return List.copyOf(result);
        }
        if (!List.of("COMMAND_CUIRASS", "RITUAL").contains(cell.style())) {
            return Step123ReferenceTeams.specializedTeam(cell.tier(), Specialization.valueOf(cell.style())).members();
        }
        final boolean ritual = cell.style().equals("RITUAL");
        final var weapon = ritual ? "ritual_staff" : "command_staff";
        var build = Step12RepresentativeBuilds.hand(cell.style(), List.of(weapon),
            ritual ? "NEW_ARCANE" : "LEATHER", ritual ? Position.BACK : Position.FRONT,
            ritual ? TargetingTactic.EXECUTIONER : TargetingTactic.WOUNDED_HUNTER);
        if (!ritual) {
            build = build.replaceItem("breastplate", "cuirass");
        }
        if (cell.tier().profilePoints() > 0) {
            build = build.withAssignments(Map.of(
                weapon, new Assignment(ritual ? ActiveEnum.ACCUMULATION : ActiveEnum.BLEEDING,
                    cell.tier().profilePoints() == 2 ? ItemRarity.UNCOMMON : ItemRarity.RARE),
                ritual ? "phantom_vestment" : "cuirass", new Assignment(ActiveEnum.SELF_HEAL,
                    cell.tier().profilePoints() == 2 ? ItemRarity.RARE : ItemRarity.LEGENDARY)
            ));
        }
        final var result = new ArrayList<Gear>();
        for (int index = 0; index < cell.size(); index++) {
            result.add(ritual && cell.size() > 1 && index == 0
                ? Step123ReferenceTeams.mainTeam(cell.tier(), 2).members().getFirst()
                : new Gear(build, EquipmentCatalogVersion.SCALING_V5));
        }
        return List.copyOf(result);
    }

    static List<Item> combatItems(Cell cell, List<Item> original) {
        return cell.mode() == Mode.RAID ? original : original.stream().map(Item::withoutStormEnhance).toList();
    }

    private static PersonageEffects sourceEffects(Cell cell) {
        return cell.effects() ? new PersonageEffects(Map.of(
            PersonageEffectType.MENU_ITEM_EFFECT,
            new PersonageEffect(new Effect.Multiplier(20, EffectCharacteristic.ATTACK), null),
            PersonageEffectType.WORKER_OF_DAY_EFFECT,
            new PersonageEffect(new Effect.Multiplier(15, EffectCharacteristic.HEALTH), null)
        )) : PersonageEffects.EMPTY;
    }

    private static PersonageEffects combatEffects(Cell cell) {
        return cell.mode() == Mode.RAID ? sourceEffects(cell) : PersonageEffects.EMPTY;
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

    static List<Cell> cases(boolean boundaries) {
        final var result = new ArrayList<Cell>();
        final var sizes = boundaries ? List.of(1, 2, 3, 7, 8, 10, 11, 13, 14, 15, 30) : List.of(1, 3, 15);
        for (final var type : RaidType.values()) {
            for (final var tier : List.of(Tier.BEGINNER, Tier.MID, Tier.STRONG)) {
                for (final int size : sizes) {
                    for (final int level : List.of(1, 10, 50)) {
                        result.add(new Cell(Mode.RAID, type.name(), "MAIN", tier, size, level, false));
                    }
                }
                result.add(new Cell(Mode.RAID, type.name(), "MAIN", tier, 3, 10, true));
                for (final var style : Specialization.values()) {
                    result.add(new Cell(Mode.RAID, type.name(), style.name(), tier, 3, 10, false));
                }
            }
        }
        for (final var template : AnomalyPveTemplate.values()) {
            for (final var mode : List.of(Mode.ANOMALY_SAFE, Mode.ANOMALY_FALLBACK)) {
                for (final var tier : List.of(Tier.BEGINNER, Tier.MID, Tier.STRONG)) {
                    for (int size = 1; size <= 5; size++) {
                        result.add(new Cell(mode, template.name(), "MAIN", tier, size, 0, false));
                    }
                    result.add(new Cell(mode, template.name(), "MAIN", tier, 3, 0, true));
                    for (final var style : Specialization.values()) {
                        result.add(new Cell(mode, template.name(), style.name(), tier, 3, 0, false));
                    }
                }
            }
        }
        return List.copyOf(result);
    }

    private static String digest(Object value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(JSON.writeValueAsBytes(value)));
    }

    private static BufferedWriter writer(Path path) throws Exception {
        return new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(
            path, StandardOpenOption.CREATE_NEW)), StandardCharsets.UTF_8));
    }

    enum Mode { RAID, ANOMALY_SAFE, ANOMALY_FALLBACK }

    record Cell(Mode mode, String encounter, String style, Tier tier, int size, int raidLevel, boolean effects) { }

    record Member(String build, Position position, String tactic, List<Item> sourceItems, List<Item> combatItems) { }

    record InputMember(String build, Position position, TargetingTactic tactic, List<Item> items) {
        InputMember {
            items = List.copyOf(items);
        }
    }

    record Prepared(List<BattlePersonage> players, GeneratedPveEnemies generated, List<Member> members) { }
}
