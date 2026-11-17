package ru.homyakin.seeker.game.battle;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
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
import java.util.UUID;
import java.util.zip.GZIPOutputStream;
import ru.homyakin.seeker.game.battle.PveAdaptationStudy.Cell;
import ru.homyakin.seeker.game.battle.PveAdaptationStudy.InputMember;
import ru.homyakin.seeker.game.battle.PveAdaptationStudy.Member;
import ru.homyakin.seeker.game.battle.PveAdaptationStudy.Mode;
import ru.homyakin.seeker.game.battle.Step123ReferenceTeams.Tier;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.RoleBuild;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;
import ru.homyakin.seeker.game.battle.targeting.TargetingTactic;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyPveTemplate;
import ru.homyakin.seeker.game.event.anomaly.generator.AnomalySafePveGenerator;
import ru.homyakin.seeker.game.event.pve.GeneratedPveEnemies;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogVersion;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffects;
import ru.homyakin.seeker.utils.RandomUtils;

/** Frozen, paired old/current anomaly formation measurement; no coefficient search or application startup. */
public final class Step192AnomalyFormationStudy {
    static final long GENERATION_ROOT = 20_261_129_000_000L;
    static final long BATTLE_ROOT = 30_261_129_000_000L;
    static final String GENERATOR = AnomalySafePveGenerator.class.getName();
    static final String EVIDENCE = "documentation/raid-redesign-step-19-2-evidence/";
    private static final List<Tier> TIERS = List.of(Tier.BEGINNER, Tier.MID, Tier.STRONG);
    private static final int ROUND_LIMIT = 2000;

    private Step192AnomalyFormationStudy() {
    }

    /** plan DIR BASELINE_CLASSES; run DIR BASELINE_CLASSES; replay ORIGINAL_DIR NEW_DIR BASELINE_CLASSES. */
    public static void main(String[] args) throws Exception {
        if ((args.length != 3 || !List.of("plan", "run", "compatibility").contains(args[0]))
            && (args.length != 4 || !args[0].equals("replay"))) {
            throw new IllegalArgumentException("Expected plan|run DIR BASELINE_CLASSES, "
                + "replay ORIGINAL_DIR NEW_DIR BASELINE_CLASSES, or compatibility NEW_JSON BASELINE_CLASSES");
        }
        final var baseline = Path.of(args[args.length - 1]).toRealPath();
        try (var loader = new BaselineLoader(baseline.toUri().toURL())) {
            final var generator = baselineGenerator(loader);
            if (args[0].equals("compatibility")) {
                historicalCompatibility(Path.of(args[1]), baseline, generator);
                return;
            }
            final var samples = cases();
            final var plans = plans(samples, generator);
            validatePreBattleGeneration(samples, plans, generator);
            final var protocol = protocol(samples, plans, baseline);
            final var output = Path.of(args[1]);
            final var protocolPath = output.resolve("protocol.json");
            if (args[0].equals("plan")) {
                Files.createDirectory(output);
                writeJson(protocolPath, protocol);
                System.out.println("planned logicalCases=136 versionCases=272 battles=7840 replayRows=96 protocolSha256="
                    + sha256(protocolPath));
                return;
            }
            requireSame(PveAdaptationStudy.JSON.readTree(protocolPath.toFile()), protocol, "frozen protocol");
            if (args[0].equals("replay")) {
                final var replay = Path.of(args[2]);
                Files.createDirectory(replay);
                try (var raw = writer(replay.resolve("battles.jsonl.gz"))) {
                    for (final var sample : samples) {
                        for (final var seed : plans.get(sample.caseId())) {
                            if (replaySelected(sample, seed.pair())) {
                                runPair(sample, seed, generator, raw);
                            }
                        }
                    }
                }
                System.out.println("replayed logicalPairs=24 versionPairs=48 battles=96");
                return;
            }
            final var summaries = new ArrayList<Map<String, Object>>();
            final var comparisons = new ArrayList<Map<String, Object>>();
            final var rawPath = output.resolve("battles.jsonl.gz");
            try (var raw = writer(rawPath)) {
                for (final var sample : samples) {
                    final var rows = new ArrayList<Map<String, Object>>();
                    for (final var seed : plans.get(sample.caseId())) {
                        rows.addAll(runPair(sample, seed, generator, raw));
                    }
                    summaries.add(summarize(sample, Version.BASELINE, rows));
                    summaries.add(summarize(sample, Version.CURRENT, rows));
                    comparisons.add(comparison(sample, rows));
                    if (sample.caseId() % 4 == 0 || sample.caseId() == samples.size()) {
                        raw.flush();
                        System.out.println("completed logicalCases=" + sample.caseId() + "/136");
                    }
                }
            }
            final var summary = new LinkedHashMap<String, Object>();
            summary.put("purpose", "FORMATION_ONLY_INITIAL_MEASUREMENT_NOT_BALANCE_ACCEPTANCE");
            summary.put("logicalCaseCount", samples.size());
            summary.put("caseCount", samples.size() * 2);
            summary.put("attemptedBattles", battleCount(samples));
            summary.put("independentLogicalPairs", battleCount(samples) / 4);
            summary.put("protocolSha256", sha256(protocolPath));
            summary.put("rawSha256", sha256(rawPath));
            summary.put("cases", summaries);
            summary.put("comparisons", comparisons);
            writeJson(output.resolve("summary.json"), summary);
            System.out.println("complete logicalCases=136 versionCases=272 battles=7840");
        }
    }

    static List<Sample> cases() {
        final var result = new ArrayList<Sample>();
        for (final var template : AnomalyPveTemplate.values()) {
            for (final var tier : TIERS) {
                for (int size = 1; size <= 5; size++) {
                    final var cell = cell(template, "MAIN", tier, size);
                    add(result, "A_MAIN", cell, 20, PveAdaptationStudy.inputFor(cell));
                }
            }
        }
        for (final var template : AnomalyPveTemplate.values()) {
            for (final var role : RoleBuild.values()) {
                final var build = Step12RepresentativeBuilds.role(role);
                add(result, "B_SOLO", cell(template, role.name(), Tier.MID, 1), 10,
                    List.of(new InputMember(build.code(), build.position(), build.tactic(),
                        new Step123BattleRunner.Gear(build, EquipmentCatalogVersion.SCALING_V5).items(0))));
            }
        }
        for (final var template : AnomalyPveTemplate.values()) {
            for (final var style : List.of("RITUAL", "COMMAND_CUIRASS")) {
                for (final int size : List.of(1, 3, 5)) {
                    final var cell = cell(template, style, Tier.MID, size);
                    add(result, "C_DIRECTIONS", cell, 10, PveAdaptationStudy.inputFor(cell));
                }
            }
        }
        for (final var template : AnomalyPveTemplate.values()) {
            for (final int size : List.of(1, 3, 5)) {
                final var members = new ArrayList<InputMember>();
                for (int index = 0; index < size; index++) {
                    members.add(new InputMember("FULL_DEFAULTS", Position.FRONT, TargetingTactic.THREAT,
                        Step1618RaidValidationStudy.defaults()));
                }
                add(result, "D_DEFAULTS", cell(template, "FULL_DEFAULTS", Tier.BEGINNER, size), 10, members);
            }
        }
        if (result.size() != 136 || battleCount(result) != 7840) {
            throw new IllegalStateException("The approved 136/272/7840 anomaly matrix changed");
        }
        return List.copyOf(result);
    }

    private static Cell cell(AnomalyPveTemplate template, String style, Tier tier, int size) {
        return new Cell(Mode.ANOMALY_SAFE, template.name(), style, tier, size, 0, false);
    }

    private static void add(List<Sample> result, String family, Cell cell, int pairs, List<InputMember> members) {
        result.add(new Sample(result.size() + 1, family, cell, pairs, members));
    }

    static int battleCount(List<Sample> samples) {
        return samples.stream().mapToInt(sample -> sample.pairs() * 4).sum();
    }

    static Map<Integer, List<Seed>> plans(List<Sample> samples, Generator baseline) {
        final var result = new TreeMap<Integer, List<Seed>>();
        for (final var sample : samples) {
            final var seeds = new ArrayList<Seed>();
            if (sample.family().equals("A_MAIN") && sample.cell().size() == 2) {
                final var branches = new TreeMap<Integer, List<Long>>();
                branches.put(2, new ArrayList<>());
                branches.put(3, new ArrayList<>());
                for (int candidate = 0; branches.values().stream().anyMatch(list -> list.size() < 10); candidate++) {
                    if (candidate >= 500) {
                        throw new IllegalStateException("Could not preselect both actual N2 generator branches");
                    }
                    final long seed = GENERATION_ROOT + sample.caseId() * 1000L + candidate;
                    final var prepared = RandomUtils.withSeed(seed,
                        () -> prepare(sample, baseline, PveAdaptationStudy.FORMULA));
                    final int count = prepared.generated().enemies().size();
                    final var branch = branches.get(count);
                    if (branch == null) {
                        throw new IllegalStateException("Unexpected actual N2 enemy count " + count);
                    }
                    if (branch.size() < 10) {
                        branch.add(seed);
                    }
                }
                for (final int count : List.of(2, 3)) {
                    for (final long generationSeed : branches.get(count)) {
                        final int pair = seeds.size();
                        seeds.add(new Seed(pair, generationSeed, BATTLE_ROOT + sample.caseId() * 1000L + pair, count));
                    }
                }
            } else {
                final int count = sample.cell().size() == 1 ? 2 : sample.cell().size();
                for (int pair = 0; pair < sample.pairs(); pair++) {
                    seeds.add(new Seed(pair, GENERATION_ROOT + sample.caseId() * 1000L + pair,
                        BATTLE_ROOT + sample.caseId() * 1000L + pair, count));
                }
            }
            require(seeds.size() == sample.pairs(), "Wrong predeclared pair count");
            result.put(sample.caseId(), List.copyOf(seeds));
        }
        return result;
    }

    static boolean replaySelected(Sample sample, int pair) {
        return sample.family().equals("A_MAIN") && sample.cell().tier() == Tier.MID
            && (pair == 0 || sample.cell().size() == 2 && pair == 10);
    }

    private static void validatePreBattleGeneration(List<Sample> samples, Map<Integer, List<Seed>> plans, Generator baseline) {
        int checks = 0;
        for (final var sample : samples) {
            for (final var seed : plans.get(sample.caseId())) {
                Map<String, Object> before = null;
                for (final var version : Version.values()) {
                    final var state = RandomUtils.withSeed(seed.generationSeed(), () -> new PreparedState(
                        prepare(sample, version == Version.BASELINE ? baseline : currentGenerator(), PveAdaptationStudy.FORMULA),
                        RandomUtils.getWithMax(1_000_000)));
                    final var prepared = state.prepared();
                    final var players = prepared.players();
                    final var enemies = prepared.generated().enemies();
                    require(enemies.size() == seed.expectedEnemyCount(), "Wrong enemy count in pre-battle validation");
                    final var row = new LinkedHashMap<String, Object>();
                    row.put("members", prepared.members());
                    row.put("sourceEffects", PersonageEffects.EMPTY.effects());
                    row.put("combatEffects", PersonageEffects.EMPTY.effects());
                    row.put("calibration", prepared.generated().calibration());
                    row.put("calibrationMetric", "LEGACY_SUM_V1");
                    row.put("generationDiagnostics", prepared.generated().diagnostics());
                    row.put("playerPower", players.stream().mapToDouble(BattlePersonage::legacyPower).sum());
                    row.put("enemyPower", enemies.stream().mapToDouble(BattlePersonage::legacyPower).sum());
                    row.put("postGenerationRandomDraw", state.nextDraw());
                    row.put("playerBasis", basis(players));
                    row.put("enemyBasis", basis(enemies));
                    row.put("playerGenerationGauges", players.stream().map(BattlePersonage::initiativeGauge).toList());
                    row.put("enemyGenerationGauges", enemies.stream().map(BattlePersonage::initiativeGauge).toList());
                    row.put("enemyCount", enemies.size());
                    final var invariant = generationInvariant(row);
                    if (before == null) {
                        before = invariant;
                    } else {
                        requireSame(before, invariant, "pre-battle generation " + sample.caseId() + "/" + seed.pair());
                    }
                }
                ++checks;
            }
        }
        require(checks == 1960, "Wrong pre-battle generation comparison count");
        System.out.println("pre-battle paired generation comparisons=1960 successful; no battles executed by validation");
    }

    private static List<Map<String, Object>> runPair(
        Sample sample, Seed seed, Generator baseline, BufferedWriter raw
    ) throws Exception {
        final var rows = new ArrayList<Map<String, Object>>();
        Map<String, Object> before = null;
        for (final var version : Version.values()) {
            Map<String, Object> firstMirror = null;
            for (final boolean reverse : List.of(false, true)) {
                final var row = runBattle(sample, seed, version, reverse, baseline);
                final var generation = generationInvariant(row);
                if (firstMirror == null) {
                    firstMirror = generation;
                } else {
                    requireSame(firstMirror, generation, "mirrored numerical generation");
                }
                if (version == Version.BASELINE && !reverse) {
                    before = generation;
                } else {
                    requireSame(before, generation, "baseline/current numerical generation");
                }
                raw.write(PveAdaptationStudy.JSON.writeValueAsString(canonicalTree(row)));
                raw.newLine();
                rows.add(row);
            }
        }
        return rows;
    }

    private static Map<String, Object> runBattle(
        Sample sample, Seed seed, Version version, boolean reverse, Generator baseline
    ) throws Exception {
        final Generator factory = version == Version.BASELINE ? baseline : currentGenerator();
        final var state = RandomUtils.withSeed(seed.generationSeed(), () -> {
            final var prepared = prepare(sample, factory, PveAdaptationStudy.FORMULA);
            return new PreparedState(prepared, RandomUtils.getWithMax(1_000_000));
        });
        final var prepared = state.prepared();
        final var players = prepared.players();
        final var enemies = prepared.generated().enemies();
        require(enemies.size() == seed.expectedEnemyCount(), "Actual generator count differs from preselected branch");
        final var row = new LinkedHashMap<String, Object>();
        row.put("caseId", sample.caseId());
        row.put("logicalCaseId", sample.caseId());
        row.put("family", sample.family());
        row.put("cell", sample.cell());
        row.put("version", version);
        row.put("pair", seed.pair());
        row.put("generationSeed", seed.generationSeed());
        row.put("battleSeed", seed.battleSeed());
        row.put("reverse", reverse);
        row.put("enemyCount", enemies.size());
        row.put("postGenerationRandomDraw", state.nextDraw());
        row.put("playerGenerationGauges", players.stream().map(BattlePersonage::initiativeGauge).toList());
        row.put("enemyGenerationGauges", enemies.stream().map(BattlePersonage::initiativeGauge).toList());
        row.put("catalog", EquipmentCatalogVersion.SCALING_V5);
        row.put("playerSkills", PveAdaptationStudy.FORMULA);
        row.put("npcSkills", PveAdaptationStudy.FORMULA);
        row.put("tempo", PveAdaptationStudy.TEMPO);
        row.put("roundLimit", ROUND_LIMIT);
        row.put("members", prepared.members());
        row.put("sourceEffects", PersonageEffects.EMPTY.effects());
        row.put("combatEffects", PersonageEffects.EMPTY.effects());
        row.put("calibration", prepared.generated().calibration());
        row.put("calibrationMetric", "LEGACY_SUM_V1");
        row.put("generationDiagnostics", prepared.generated().diagnostics());
        row.put("playerPower", players.stream().mapToDouble(BattlePersonage::legacyPower).sum());
        row.put("enemyPower", enemies.stream().mapToDouble(BattlePersonage::legacyPower).sum());
        row.put("playerBasis", basis(players));
        row.put("enemyBasis", basis(enemies));
        try {
            final var result = RandomUtils.withSeed(seed.battleSeed(), () -> new Battle(seed.battleSeed()).process(
                reverse ? players : enemies, reverse ? enemies : players, ROUND_LIMIT, PveAdaptationStudy.TEMPO));
            final long alivePlayers = players.stream().filter(BattlePersonage::isAlive).count();
            final long aliveEnemies = enemies.stream().filter(BattlePersonage::isAlive).count();
            row.put("outcome", alivePlayers > 0 ? "WIN" : aliveEnemies > 0 ? "LOSS" : "DRAW");
            row.put("rounds", result.rounds());
            row.put("actualEnemyPositions", enemies.stream().map(enemy -> result.initState().lines()
                .get(result.initState().personagesById().get(enemy.id()).lineIndex()).position()).toList());
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
            row.put("enemyActionCounts", enemyActions(result.actionLog(), enemies));
            if (replaySelected(sample, seed.pair())) {
                row.put("actionLog", result.actionLog());
                row.put("traceEvents", result.actionLog().traceEvents());
            }
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

    static PveAdaptationStudy.Prepared prepare(Sample sample, Generator generator, SkillFormulaVersion formula) {
        final var players = new ArrayList<BattlePersonage>();
        final var members = new ArrayList<Member>();
        for (final var input : sample.members()) {
            final var combat = input.items().stream().map(Item::withoutStormEnhance).toList();
            final var actor = BattlePersonage.forCombat(combat, input.position(), PersonageEffects.EMPTY,
                Optional.of(input.build()), formula);
            actor.setTargetingTactic(input.tactic());
            players.add(actor);
            members.add(new Member(input.build(), input.position(), input.tactic().name(), input.items(), combat));
        }
        final var generated = generator.generate(AnomalyPveTemplate.valueOf(sample.cell().encounter()), players, formula);
        if (formula == PveAdaptationStudy.FORMULA) {
            for (final var actor : java.util.stream.Stream.concat(players.stream(), generated.enemies().stream()).toList()) {
                require(actor.usesScalingCombatRules() && actor.skillSnapshots().stream()
                    .allMatch(skill -> skill.formulaVersion() == formula), "Non-V3 participant");
            }
        }
        return new PveAdaptationStudy.Prepared(List.copyOf(players), generated, List.copyOf(members));
    }

    static List<Map<String, Object>> basis(List<BattlePersonage> team) {
        return team.stream().map(Step192AnomalyFormationStudy::snapshot).toList();
    }

    private static Map<String, Object> snapshot(BattlePersonage actor) {
        final var result = new LinkedHashMap<String, Object>();
        result.put("id", actor.id());
        result.put("name", actor.name());
        result.put("health", actor.health());
        result.put("maxHealth", actor.maxHealth());
        result.put("attack", actor.battlePersonageStats().attack());
        result.put("defense", actor.battlePersonageStats().defense());
        result.put("declaredPosition", actor.startPosition());
        result.put("range", actor.range());
        result.put("attackTypes", actor.attackTypes().stream().sorted().toList());
        result.put("attacksByRange", actor.attacksByRange());
        result.put("defenses", actor.defenses());
        result.put("defenseReductions", actor.defenseReductions());
        result.put("critChance", actor.critChance());
        result.put("dodgeChance", actor.dodgeChance());
        result.put("critMultiplier", actor.critMultiplier());
        result.put("initiative", actor.initiative());
        result.put("initiativeGauge", actor.initiativeGauge());
        result.put("readyToAct", actor.readyToAct());
        result.put("threat", actor.totalThreat());
        result.put("impactStrength", actor.impactStrength());
        result.put("skills", actor.skillSnapshots());
        result.put("items", actor.itemSnapshots());
        result.put("tactic", actor.targetingTactic());
        result.put("tempo", actor.battleTempoVersion());
        result.put("legacyPower", actor.legacyPower());
        return result;
    }

    private static Map<String, Object> generationInvariant(Map<String, Object> row) {
        final var result = new LinkedHashMap<String, Object>();
        for (final var key : List.of("members", "sourceEffects", "combatEffects", "calibration", "calibrationMetric",
            "generationDiagnostics", "playerPower", "enemyPower", "postGenerationRandomDraw", "playerBasis",
            "playerGenerationGauges", "enemyGenerationGauges", "enemyCount")) {
            result.put(key, row.get(key));
        }
        final var enemies = new ArrayList<Map<String, Object>>();
        for (final var enemy : (List<?>) row.get("enemyBasis")) {
            final var numerical = new LinkedHashMap<>((Map<String, Object>) enemy);
            numerical.remove("range");
            numerical.remove("attacksByRange");
            numerical.remove("items");
            enemies.add(numerical);
        }
        result.put("enemyNumericalBasis", enemies);
        return result;
    }

    private static Map<String, Object> summarize(Sample sample, Version version, List<Map<String, Object>> all) {
        final var rows = all.stream().filter(row -> row.get("version") == version).toList();
        final var outcomes = new TreeMap<String, Integer>();
        final var byOrder = new TreeMap<String, Map<String, Integer>>();
        final var statuses = new TreeMap<String, Integer>();
        final var metrics = new TreeMap<String, List<Double>>();
        final var skills = new TreeMap<String, Long>();
        final var kinds = new TreeMap<String, Long>();
        final var byCount = new TreeMap<Integer, Map<String, Integer>>();
        final var pairScores = pairScores(sample, version, all);
        final var errors = new ArrayList<Double>();
        for (final var row : rows) {
            final var outcome = (String) row.get("outcome");
            outcomes.merge(outcome, 1, Integer::sum);
            byOrder.computeIfAbsent((Boolean) row.get("reverse") ? "PLAYERS_FIRST" : "ENEMIES_FIRST",
                unused -> new TreeMap<>()).merge(outcome, 1, Integer::sum);
            byCount.computeIfAbsent((Integer) row.get("enemyCount"), unused -> new TreeMap<>())
                .merge(outcome, 1, Integer::sum);
            final var calibration = (ru.homyakin.seeker.game.event.pve.PveCharacteristicScaling.Result) row.get("calibration");
            statuses.merge(calibration.status().name(), 1, Integer::sum);
            if (calibration.targetPower() > 0) {
                errors.add(Math.abs(calibration.achievedPower() / calibration.targetPower() - 1));
            }
            if (!outcome.equals("TIMEOUT")) {
                for (final var key : List.of("rounds", "actionCount", "normalAttempts", "moves", "forcedMoves")) {
                    metric(metrics, key, (Number) row.get(key));
                }
                for (final var side : List.of("playerTotals", "enemyTotals")) {
                    ((Map<String, Number>) row.get(side)).forEach((key, value) -> metric(metrics, side + "." + key, value));
                }
                mergeCounts(skills, (Map<?, ?>) row.get("skillEvents"));
                mergeCounts(kinds, (Map<?, ?>) row.get("skillEventsByKind"));
            }
        }
        final var result = new LinkedHashMap<String, Object>();
        result.put("caseId", sample.caseId());
        result.put("version", version);
        result.put("family", sample.family());
        result.put("cell", sample.cell());
        result.put("pairs", sample.pairs());
        result.put("attemptedBattles", rows.size());
        result.put("completedBattles", rows.size() - outcomes.getOrDefault("TIMEOUT", 0));
        result.put("outcomes", outcomes);
        result.put("outcomesByOrder", byOrder);
        result.put("outcomesByEnemyCount", byCount);
        result.put("playerWinsPerAttempt", (double) outcomes.getOrDefault("WIN", 0) / rows.size());
        result.put("calibrationStatuses", statuses);
        result.put("skillEvents", skills);
        result.put("skillEventsByKind", kinds);
        result.put("pairScoresByIndex", pairScores);
        final var completeScores = pairScores.stream().filter(java.util.Objects::nonNull).toList();
        result.put("completePairs", completeScores.size());
        result.put("incompletePairs", sample.pairs() - completeScores.size());
        result.put("pairedScore", distribution(completeScores));
        if (!completeScores.isEmpty()) {
            result.put("pairedScore95Hoeffding", confidence(completeScores, false));
        }
        final var distributions = new TreeMap<String, Object>();
        metrics.forEach((key, values) -> distributions.put(key, distribution(values)));
        result.put("completedBattleMetrics", distributions);
        result.put("calibrationAbsoluteRelativeError", distribution(errors));
        return result;
    }

    private static List<Double> pairScores(Sample sample, Version version, List<Map<String, Object>> rows) {
        final var scores = new ArrayList<Double>();
        for (int pair = 0; pair < sample.pairs(); pair++) {
            final int index = pair;
            final var mirrors = rows.stream().filter(row -> row.get("version") == version
                && (Integer) row.get("pair") == index).toList();
            require(mirrors.size() == 2, "Pair must contain both mirrors");
            scores.add(mirrors.stream().anyMatch(row -> row.get("outcome").equals("TIMEOUT")) ? null
                : mirrors.stream().mapToDouble(row -> switch ((String) row.get("outcome")) {
                    case "WIN" -> 0.5;
                    case "DRAW" -> 0.25;
                    case "LOSS" -> 0.0;
                    default -> throw new IllegalStateException("Unexpected outcome");
                }).sum());
        }
        return scores;
    }

    private static Map<String, Object> comparison(Sample sample, List<Map<String, Object>> rows) {
        final var before = pairScores(sample, Version.BASELINE, rows);
        final var after = pairScores(sample, Version.CURRENT, rows);
        final var deltas = new ArrayList<Double>();
        for (int pair = 0; pair < before.size(); pair++) {
            deltas.add(before.get(pair) == null || after.get(pair) == null ? null : after.get(pair) - before.get(pair));
        }
        final var complete = deltas.stream().filter(java.util.Objects::nonNull).toList();
        final var result = new LinkedHashMap<String, Object>();
        result.put("logicalCaseId", sample.caseId());
        result.put("family", sample.family());
        result.put("cell", sample.cell());
        result.put("pairedDeltas", deltas);
        result.put("distribution", distribution(complete));
        if (!complete.isEmpty()) {
            result.put("pairedDelta95Hoeffding", confidence(complete, true));
        }
        return result;
    }

    private static Map<String, Double> confidence(List<Double> values, boolean delta) {
        final double mean = values.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
        final double halfWidth = delta ? Math.sqrt(2.0 * Math.log(40.0) / values.size())
            : Math.sqrt(Math.log(40.0) / (2.0 * values.size()));
        return Map.of("low", Math.max(delta ? -1 : 0, mean - halfWidth), "high", Math.min(1, mean + halfWidth),
            "unclippedHalfWidth", halfWidth);
    }

    private static void metric(Map<String, List<Double>> metrics, String key, Number value) {
        metrics.computeIfAbsent(key, unused -> new ArrayList<>()).add(value.doubleValue());
    }

    static Map<String, Object> distribution(List<Double> values) {
        if (values.isEmpty()) {
            return Map.of("count", 0);
        }
        final var sorted = values.stream().sorted().toList();
        return Map.of("count", sorted.size(), "mean", sorted.stream().mapToDouble(Double::doubleValue).average().orElseThrow(),
            "min", sorted.getFirst(), "p50", percentile(sorted, 0.5), "p90", percentile(sorted, 0.9),
            "p95", percentile(sorted, 0.95), "max", sorted.getLast());
    }

    private static double percentile(List<Double> sorted, double quantile) {
        return sorted.get(Math.max(0, (int) Math.ceil(quantile * sorted.size()) - 1));
    }

    private static Map<String, Object> protocol(List<Sample> samples, Map<Integer, List<Seed>> plans, Path baseline)
        throws Exception {
        final var result = new LinkedHashMap<String, Object>();
        result.put("protocolVersion", "STEP_19_2_FORMATION_INITIAL_MEASUREMENT_2026_11_15_V1");
        result.put("purpose", "FORMATION_ONLY_INITIAL_MEASUREMENT_NOT_BALANCE_ACCEPTANCE");
        result.put("logicalCaseCount", samples.size());
        result.put("caseCount", samples.size() * 2);
        result.put("attemptedBattles", battleCount(samples));
        result.put("independentLogicalPairs", battleCount(samples) / 4);
        result.put("preBattleGenerationComparisons", 1960);
        result.put("versions", Version.values());
        result.put("mode", Mode.ANOMALY_SAFE);
        result.put("serviceCaveat", "One shared generator/Battle path; actual SAFE/FALLBACK service routes tested separately");
        result.put("metric", "LEGACY_SUM_V1");
        result.put("safePowerRatio", 0.92);
        result.put("catalog", EquipmentCatalogVersion.SCALING_V5);
        result.put("playerSkills", PveAdaptationStudy.FORMULA);
        result.put("npcSkills", PveAdaptationStudy.FORMULA);
        result.put("tempo", PveAdaptationStudy.TEMPO);
        result.put("tempoDamageMultiplier", PveAdaptationStudy.TEMPO.damageMultiplier());
        result.put("roundLimit", ROUND_LIMIT);
        result.put("generationRoot", GENERATION_ROOT);
        result.put("battleRoot", BATTLE_ROOT);
        result.put("seedPolicy", "root + caseId*1000 + pair, except A_MAIN N2 generation candidates preselected "
            + "through baseline actual generator in ascending candidate order: first ten K2 then first ten K3; "
            + "no outcomes consulted, no enemy count overridden; same seeds for versions and mirrors");
        result.put("inferencePolicy", "Mirrors are dependent; 1960 logical pair indices across versions, "
            + "not 7840 independent trials. No 50-percent acceptance threshold in this initial measurement");
        result.put("durationPolicy", "Completed battles only; rounds/actions/turns are not seconds");
        result.put("timeoutPolicy", "TIMEOUT is not LOSS; incomplete pairs excluded from paired scores");
        result.put("skillEventPolicy", "Event counts by source side, skill and kind, not unique activations");
        result.put("canonicalizationPolicy", "Only slots and availableOnSlots arrays are semantic sets");
        result.put("baselineClassSha256", classHash(baseline));
        result.put("currentClassSha256", currentClassHash());
        final var baselineClasses = baselineClassHashes(baseline);
        result.put("baselineClassSha256ByPath", baselineClasses);
        result.put("currentClassSha256ByPath", currentClassHashes(baselineClasses.keySet()));
        result.put("childFirstClasses", baselineClasses.keySet().stream()
            .map(path -> path.substring(0, path.length() - ".class".length()).replace('/', '.')).toList());
        final var metadata = PveAdaptationStudy.JSON.readTree(Path.of(EVIDENCE + "baseline-metadata.json").toFile());
        final var classArchive = metadata.get("classArchive").asText();
        final var sourceArchive = metadata.get("sourceArchive").asText();
        result.put("baselineClassArchivePath", classArchive);
        result.put("baselineClassArchiveSha256", sha256(Path.of(classArchive)));
        result.put("baselineSourceArchivePath", sourceArchive);
        result.put("baselineSourceArchiveSha256", sha256(Path.of(sourceArchive)));
        result.put("publicSourceSha256", sourceHashes());
        result.put("replayPairs", samples.stream().flatMap(sample -> plans.get(sample.caseId()).stream()
            .filter(seed -> replaySelected(sample, seed.pair())).map(seed -> Map.of("caseId", sample.caseId(),
                "pair", seed.pair()))).toList());
        result.put("cases", samples.stream().map(sample -> Map.of("sample", sample,
            "seeds", plans.get(sample.caseId()))).toList());
        return result;
    }

    private static Map<String, String> sourceHashes() throws Exception {
        final var paths = new ArrayList<Path>();
        for (final var root : List.of("game/src/main/java/ru/homyakin/seeker/game/battle",
            "game/src/main/java/ru/homyakin/seeker/game/item", "game/src/main/java/ru/homyakin/seeker/game/personage/models",
            "game/src/main/java/ru/homyakin/seeker/game/personage/power", "game/src/main/java/ru/homyakin/seeker/game/effect",
            "game/src/main/java/ru/homyakin/seeker/game/event/pve", "game/src/main/java/ru/homyakin/seeker/utils",
            "game/src/main/java/ru/homyakin/seeker/game/event/anomaly/generator",
            "game/src/main/java/ru/homyakin/seeker/game/event/anomaly/entity", "game/src/main/resources/game-data/catalog",
            "game/src/test/java/ru/homyakin/seeker/game/battle/simulation")) {
            if (Files.isDirectory(Path.of(root))) {
                try (var walk = Files.walk(Path.of(root))) {
                    paths.addAll(walk.filter(Files::isRegularFile).toList());
                }
            }
        }
        for (final var path : List.of("game/src/test/java/ru/homyakin/seeker/game/battle/Step192AnomalyFormationStudy.java",
            "game/src/test/java/ru/homyakin/seeker/game/battle/Step192AnomalyFormationStudyTest.java",
            "game/src/test/java/ru/homyakin/seeker/game/battle/Step123BattleRunner.java",
            "game/src/test/java/ru/homyakin/seeker/game/battle/Step12RepresentativeBuilds.java",
            "game/src/test/java/ru/homyakin/seeker/game/battle/Step123ReferenceTeams.java",
            "game/src/test/java/ru/homyakin/seeker/game/battle/PveAdaptationStudy.java",
            "game/src/test/java/ru/homyakin/seeker/game/battle/Step1618RaidValidationStudy.java",
            "game/src/test/java/ru/homyakin/seeker/game/battle/Step123BattleStudy.java",
            "game/src/main/java/ru/homyakin/seeker/game/event/anomaly/action/AnomalyBattleService.java",
            "game/src/test/java/ru/homyakin/seeker/game/event/anomaly/AnomalyBattleServiceCombatVersionTest.java",
            "game/src/test/java/ru/homyakin/seeker/game/event/anomaly/generator/AnomalySafePveGeneratorTest.java",
            "game/src/test/java/ru/homyakin/seeker/game/event/anomaly/generator/AnomalyV3FormationTest.java",
            "documentation/raid-redesign-step-19-1-anomaly-design.md", EVIDENCE + "protocol.md",
            EVIDENCE + "validate-study.mjs", EVIDENCE + "baseline-metadata.json")) {
            paths.add(Path.of(path));
        }
        final var hashes = new TreeMap<String, String>();
        for (final var path : paths.stream().distinct().sorted().toList()) {
            hashes.put(path.toString(), sha256(path));
        }
        return hashes;
    }

    private static void historicalCompatibility(Path output, Path baseline, Generator old) throws Exception {
        final var rows = new ArrayList<Map<String, Object>>();
        final var sizes = List.of(0, 1, 2, 3, 4, 5, 6, 128);
        final var versions = List.of(SkillFormulaVersion.LEGACY_SKILLS_V1, SkillFormulaVersion.SCALING_SKILLS_V1,
            SkillFormulaVersion.SCALING_SKILLS_V2);
        for (final var template : AnomalyPveTemplate.values()) {
            for (final var formula : versions) {
                for (final int size : sizes) {
                    for (int index = 0; index < 2; index++) {
                        final long seed = GENERATION_ROOT + 9_000_000 + template.ordinal() * 100_000L
                            + formula.ordinal() * 10_000L + size * 10L + index;
                        final var before = historicalSnapshot(old, template, formula, size, seed);
                        final var after = historicalSnapshot(currentGenerator(), template, formula, size, seed);
                        requireSame(before, after, "historical compatibility " + template + "/" + formula + "/" + size);
                        rows.add(Map.of("template", template, "formula", formula, "size", size, "seed", seed,
                            "baselineSha256", digest(before), "currentSha256", digest(after), "snapshot", before));
                    }
                }
            }
        }
        require(rows.size() == 192, "Wrong historical compatibility matrix");
        writeJson(output, Map.of("purpose", "EXACT_HISTORICAL_GENERATION_COMPATIBILITY_NOT_NEW_BATTLES",
            "checks", rows.size(), "legalInputChecks", 120, "normalizationChecks", 72,
            "baselineClassSha256", classHash(baseline), "currentClassSha256", currentClassHash(),
            "baselineClassSha256ByPath", baselineClassHashes(baseline),
            "currentClassSha256ByPath", currentClassHashes(baselineClassHashes(baseline).keySet()), "comparisons", rows));
        System.out.println("historical compatibility checks=192 legal=120 normalization=72");
    }

    private static Map<String, Object> historicalSnapshot(
        Generator generator, AnomalyPveTemplate template, SkillFormulaVersion formula, int size, long seed
    ) {
        return RandomUtils.withSeed(seed, () -> {
            final var gear = Step123ReferenceTeams.mainTeam(Tier.MID, 1).members().getFirst();
            final var players = new ArrayList<BattlePersonage>();
            for (int index = 0; index < size; index++) {
                players.add(BattlePersonage.forCombat(gear.items(0), gear.build().position(), PersonageEffects.EMPTY,
                    Optional.empty(), PveAdaptationStudy.FORMULA));
            }
            final var generated = generator.generate(template, players, formula);
            return Map.of("calibration", generated.calibration(), "diagnostics", generated.diagnostics(),
                "players", basis(players), "enemies", basis(generated.enemies()),
                "postGenerationRandomDraw", RandomUtils.getWithMax(1_000_000));
        });
    }

    static Generator currentGenerator() {
        return new AnomalySafePveGenerator()::generateCalibrated;
    }

    static Generator baselineGenerator(BaselineLoader loader) throws Exception {
        require(loader.loadClass(BattlePersonage.class.getName()) == BattlePersonage.class, "Battle actors must be shared");
        require(loader.loadClass(RandomUtils.class.getName()) == RandomUtils.class, "Random scope must be shared");
        final var type = loader.loadClass(GENERATOR);
        require(type.getClassLoader() == loader, "Baseline generator cannot fall back to current generator");
        require(loader.loadClass(GENERATOR + "$1").getClassLoader() == loader,
            "Baseline synthetic switch class cannot fall back to current code");
        final var object = type.getConstructor().newInstance();
        final var method = type.getMethod("generateCalibrated", AnomalyPveTemplate.class, List.class, SkillFormulaVersion.class);
        return (template, players, formula) -> {
            try {
                return (GeneratedPveEnemies) method.invoke(object, template, players, formula);
            } catch (InvocationTargetException failure) {
                if (failure.getCause() instanceof RuntimeException runtime) {
                    throw runtime;
                }
                throw new IllegalStateException("Baseline generator failed", failure.getCause());
            } catch (ReflectiveOperationException failure) {
                throw new IllegalStateException("Cannot invoke baseline generator", failure);
            }
        };
    }

    private static String classHash(Path baseline) throws Exception {
        return sha256(baseline.resolve(GENERATOR.replace('.', '/') + ".class"));
    }

    private static Map<String, String> baselineClassHashes(Path baseline) throws Exception {
        final var result = new TreeMap<String, String>();
        final var mainPath = Path.of(GENERATOR.replace('.', '/') + ".class");
        final var parent = baseline.resolve(mainPath).getParent();
        final var stem = mainPath.getFileName().toString().replace(".class", "");
        try (var files = Files.list(parent)) {
            for (final var file : files.filter(Files::isRegularFile).toList()) {
                final var name = file.getFileName().toString();
                if (name.equals(stem + ".class") || name.startsWith(stem + "$") && name.endsWith(".class")) {
                    result.put(baseline.relativize(file).toString(), sha256(file));
                }
            }
        }
        require(result.containsKey(mainPath.toString())
            && result.containsKey(GENERATOR.replace('.', '/') + "$1.class"), "Incomplete baseline generator class family");
        return result;
    }

    private static Map<String, String> currentClassHashes(java.util.Set<String> classPaths) throws Exception {
        final var result = new TreeMap<String, String>();
        for (final var path : classPaths) {
            try (var input = Step192AnomalyFormationStudy.class.getClassLoader().getResourceAsStream(path)) {
                require(input != null, "Missing current generator family class " + path);
                result.put(path, bytesHash(input.readAllBytes()));
            }
        }
        return result;
    }

    private static String currentClassHash() throws Exception {
        try (var input = Step192AnomalyFormationStudy.class.getClassLoader()
            .getResourceAsStream(GENERATOR.replace('.', '/') + ".class")) {
            require(input != null, "Missing current generator class");
            return bytesHash(input.readAllBytes());
        }
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

    private static Map<String, Object> enemyActions(BattleActionLog log, List<BattlePersonage> enemies) {
        final var result = new TreeMap<String, Object>();
        for (final var enemy : enemies) {
            final var id = enemy.id();
            result.put(id.toString(), Map.of("normalAttempts", log.traceEvents().stream()
                    .filter(BattleTraceEvent.NormalAttackAttempt.class::isInstance)
                    .map(BattleTraceEvent.NormalAttackAttempt.class::cast).filter(event -> event.attackerId().equals(id)).count(),
                "turns", log.traceEvents().stream().filter(BattleTraceEvent.TurnStarted.class::isInstance)
                    .map(BattleTraceEvent.TurnStarted.class::cast).filter(event -> event.personageId().equals(id)).count(),
                "moves", log.events().stream().filter(BattleEvent.MovedTowardEnemy.class::isInstance)
                    .map(BattleEvent.MovedTowardEnemy.class::cast).filter(event -> event.personageId().equals(id)).count(),
                "damage", enemy.battlePersonageStats().damageDealt()));
        }
        return result;
    }

    private static SkillCounters skillCounters(List<BattleEvent> events, List<BattlePersonage> players,
                                              List<BattlePersonage> enemies) {
        final var playerIds = players.stream().map(BattlePersonage::id).collect(java.util.stream.Collectors.toSet());
        final var enemyIds = enemies.stream().map(BattlePersonage::id).collect(java.util.stream.Collectors.toSet());
        final var counts = new TreeMap<String, Long>();
        final var kinds = new TreeMap<String, Long>();
        for (final var side : List.of("PLAYER", "ENEMY")) {
            for (final var skill : ActiveEnum.values()) {
                counts.put(side + ":" + skill, 0L);
            }
        }
        for (final var event : events) {
            final var observation = observation(event);
            if (observation == null) {
                continue;
            }
            final String side;
            if (playerIds.contains(observation.source())) {
                side = "PLAYER";
            } else if (enemyIds.contains(observation.source())) {
                side = "ENEMY";
            } else {
                throw new IllegalStateException("Unknown skill event source");
            }
            final var key = side + ":" + observation.skill();
            counts.merge(key, 1L, Long::sum);
            kinds.merge(key + ":" + observation.kind(), 1L, Long::sum);
        }
        return new SkillCounters(counts, kinds);
    }

    private static SkillObservation observation(BattleEvent event) {
        return switch (event) {
            case BattleEvent.SkillWindowUsed used -> new SkillObservation(used.personageId(), used.skill(), "WINDOW");
            case BattleEvent.ScalingSkillDamage damage -> new SkillObservation(damage.sourceId(), damage.skill(),
                damage.periodic() ? "SCALING_PERIODIC_DAMAGE" : "SCALING_DIRECT_DAMAGE");
            case BattleEvent.SkillDamage damage -> new SkillObservation(damage.sourceId(), damage.skill(), "LEGACY_DIRECT_DAMAGE");
            case BattleEvent.EffectDamage damage -> new SkillObservation(damage.sourceId(), damage.skill(), "LEGACY_PERIODIC_DAMAGE");
            case BattleEvent.PersonageHealed heal -> new SkillObservation(heal.personageId(), heal.skill(), "HEAL");
            case BattleEvent.PersonageAttackBuffed buff -> new SkillObservation(buff.personageId(), buff.skill(), "BUFF");
            case BattleEvent.PersonageForcedMove move -> new SkillObservation(move.sourceId(), move.skill(), "FORCED_MOVE");
            default -> null;
        };
    }

    private static void mergeCounts(Map<String, Long> destination, Map<?, ?> source) {
        source.forEach((key, value) -> destination.merge((String) key, ((Number) value).longValue(), Long::sum));
    }

    static JsonNode canonicalTree(Object value) {
        return Step1618RaidValidationStudy.canonicalTree(value);
    }

    private static void requireSame(Object before, Object after, String label) {
        require(canonicalTree(before).equals(canonicalTree(after)), "Different " + label);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static BufferedWriter writer(Path path) throws Exception {
        return new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(
            path, StandardOpenOption.CREATE_NEW)), StandardCharsets.UTF_8));
    }

    private static void writeJson(Path path, Object value) throws Exception {
        Files.writeString(path, PveAdaptationStudy.JSON.writerWithDefaultPrettyPrinter().writeValueAsString(canonicalTree(value)),
            StandardOpenOption.CREATE_NEW);
    }

    private static String digest(Object value) throws Exception {
        return bytesHash(PveAdaptationStudy.JSON.writeValueAsBytes(canonicalTree(value)));
    }

    private static String bytesHash(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static String sha256(Path path) throws Exception {
        return bytesHash(Files.readAllBytes(path));
    }

    record Sample(int caseId, String family, Cell cell, int pairs, List<InputMember> members) {
        Sample {
            members = List.copyOf(members);
            require(members.size() == cell.size(), "Input count differs from declared size");
            for (final var member : members) {
                final var occupied = java.util.EnumSet.noneOf(PersonageSlot.class);
                for (final var item : member.items()) {
                    for (final var slot : item.object().slots()) {
                        require(occupied.add(slot), "Overlapping equipment slots in " + member.build());
                    }
                }
                require(occupied.equals(java.util.EnumSet.allOf(PersonageSlot.class)), "Incomplete equipment " + member.build());
            }
        }
    }

    record Seed(int pair, long generationSeed, long battleSeed, int expectedEnemyCount) { }

    enum Version { BASELINE, CURRENT }

    @FunctionalInterface
    interface Generator {
        GeneratedPveEnemies generate(AnomalyPveTemplate template, List<BattlePersonage> players, SkillFormulaVersion formula);
    }

    private record PreparedState(PveAdaptationStudy.Prepared prepared, int nextDraw) { }
    private record SkillObservation(UUID source, ActiveEnum skill, String kind) { }
    private record SkillCounters(Map<String, Long> events, Map<String, Long> byKind) { }

    static final class BaselineLoader extends URLClassLoader {
        BaselineLoader(URL directory) {
            super(new URL[]{directory}, Step192AnomalyFormationStudy.class.getClassLoader());
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            synchronized (getClassLoadingLock(name)) {
                if (!name.equals(GENERATOR) && !name.startsWith(GENERATOR + "$")) {
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
