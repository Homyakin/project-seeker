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
import java.util.TreeMap;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;
import ru.homyakin.seeker.game.battle.PveAdaptationStudy.Cell;
import ru.homyakin.seeker.game.battle.PveAdaptationStudy.InputMember;
import ru.homyakin.seeker.game.battle.PveAdaptationStudy.Mode;
import ru.homyakin.seeker.game.battle.Step123ReferenceTeams.Tier;
import ru.homyakin.seeker.game.battle.Step192AnomalyFormationStudy.Generator;
import ru.homyakin.seeker.game.battle.Step192AnomalyFormationStudy.Sample;
import ru.homyakin.seeker.game.battle.Step192AnomalyFormationStudy.Seed;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyPveTemplate;
import ru.homyakin.seeker.game.event.pve.PveCharacteristicScaling;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogVersion;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffects;
import ru.homyakin.seeker.utils.RandomUtils;

/** Preregistered coefficient screening and independent-seed confirmation; no application startup. */
public final class Step193AnomalyDifficultyStudy {
    static final List<String> SCREEN_RATIOS = List.of("0.92", "1.10", "1.30", "1.50", "1.75", "2.00", "2.40");
    static final String BASELINE_RATIO = "0.92";
    static final String EVIDENCE = "documentation/raid-redesign-step-19-3-evidence/";
    private static final int ROUND_LIMIT = 2000;
    private static final String GENERATOR = Step192AnomalyFormationStudy.GENERATOR;

    private Step193AnomalyDifficultyStudy() { }

    /** plan|run PHASE DIR CANDIDATES_ROOT [SELECTION_JSON]; replay PHASE ORIGINAL NEW ROOT [SELECTION_JSON]. */
    public static void main(String[] args) throws Exception {
        require(args.length >= 4 && List.of("plan", "run", "replay").contains(args[0]), "Invalid command");
        final var phase = Phase.valueOf(args[1]);
        final boolean replay = args[0].equals("replay");
        require(args.length == (replay ? 5 : 4) + (phase == Phase.CONFIRM ? 1 : 0), "Invalid argument count");
        final var output = Path.of(args[2]);
        final var candidateRoot = Path.of(args[replay ? 4 : 3]).toRealPath();
        final Path selectionPath = phase == Phase.CONFIRM ? Path.of(args[args.length - 1]) : null;
        final var selection = selectionPath == null ? Map.<String, Object>of() : selection(selectionPath);
        final var ratios = ratios(phase, selection);
        final var samples = cases(phase);
        final var plans = plans(phase, samples);
        try (var candidates = new Candidates(candidateRoot, ratios)) {
            preflight(phase, samples, plans, candidates, selection);
            final var protocol = protocol(phase, samples, plans, candidates, selectionPath, selection);
            final var protocolPath = output.resolve("protocol.json");
            if (args[0].equals("plan")) {
                Files.createDirectory(output);
                writeJson(protocolPath, protocol);
                System.out.println("planned phase=" + phase + " cases=72 ratioCases=" + samples.size() * ratios.size()
                    + " battles=" + battleCount(phase, ratios.size()) + " replayRows=" + replayCount(phase, ratios.size()));
                return;
            }
            requireSame(PveAdaptationStudy.JSON.readTree(protocolPath.toFile()), protocol, "frozen protocol");
            final var destination = replay ? Path.of(args[3]) : output;
            if (replay) {
                Files.createDirectory(destination);
            }
            final var summaries = new ArrayList<Map<String, Object>>();
            final var comparisons = new ArrayList<Map<String, Object>>();
            final var rawPath = destination.resolve("battles.jsonl.gz");
            try (var raw = writer(rawPath)) {
                for (final var sample : samples) {
                    final var rows = new ArrayList<Map<String, Object>>();
                    for (final var seed : plans.get(sample.caseId())) {
                        if (!replay || replaySelected(phase, sample, seed.pair())) {
                            rows.addAll(runPair(phase, sample, seed, candidates, selection, raw));
                        }
                    }
                    if (!replay) {
                        for (final var ratio : ratios) {
                            summaries.add(summarize(sample, ratio, rows));
                            if (!ratio.equals(BASELINE_RATIO)) {
                                comparisons.add(comparison(sample, ratio, rows));
                            }
                        }
                        if (sample.caseId() % 4 == 0) {
                            raw.flush();
                            System.out.println("completed phase=" + phase + " logicalCases=" + sample.caseId() + "/72");
                        }
                    }
                }
            }
            if (!replay) {
                final var summary = new LinkedHashMap<String, Object>();
                summary.put("purpose", "ANOMALY_DIFFICULTY_" + phase);
                summary.put("phase", phase);
                summary.put("ratios", ratios);
                summary.put("logicalCaseCount", samples.size());
                summary.put("caseCount", samples.size() * ratios.size());
                summary.put("attemptedBattles", battleCount(phase, ratios.size()));
                summary.put("independentLogicalPairs", samples.size() * phase.pairs);
                summary.put("protocolSha256", sha256(protocolPath));
                summary.put("rawSha256", sha256(rawPath));
                summary.put("cases", summaries);
                summary.put("comparisons", comparisons);
                writeJson(destination.resolve("summary.json"), summary);
            }
            System.out.println("complete phase=" + phase + " replay=" + replay + " battles="
                + (replay ? replayCount(phase, ratios.size()) : battleCount(phase, ratios.size())));
        }
    }

    static List<Sample> cases(Phase phase) {
        final var result = new ArrayList<Sample>();
        for (final var template : AnomalyPveTemplate.values()) {
            for (final var tier : List.of(Tier.BEGINNER, Tier.MID, Tier.STRONG)) {
                for (final int size : List.of(3, 4, 5)) {
                    final var cell = cell(template, "MAIN", tier, size);
                    add(result, phase, "A_MAIN", cell, PveAdaptationStudy.inputFor(cell));
                }
            }
        }
        for (final var template : AnomalyPveTemplate.values()) {
            for (final var style : List.of("RITUAL", "COMMAND_CUIRASS")) {
                for (final int size : List.of(3, 4, 5)) {
                    final var cell = cell(template, style, Tier.MID, size);
                    add(result, phase, "C_DIRECTIONS", cell, PveAdaptationStudy.inputFor(cell));
                }
            }
        }
        for (final var template : AnomalyPveTemplate.values()) {
            for (final int size : List.of(3, 4, 5)) {
                final var members = new ArrayList<InputMember>();
                for (int index = 0; index < size; index++) {
                    members.add(new InputMember("FULL_DEFAULTS", Position.FRONT,
                        ru.homyakin.seeker.game.battle.targeting.TargetingTactic.THREAT,
                        Step1618RaidValidationStudy.defaults()));
                }
                add(result, phase, "D_DEFAULTS", cell(template, "FULL_DEFAULTS", Tier.BEGINNER, size), members);
            }
        }
        require(result.size() == 72, "Approved 72-case matrix changed");
        return List.copyOf(result);
    }

    private static Cell cell(AnomalyPveTemplate template, String style, Tier tier, int size) {
        return new Cell(Mode.ANOMALY_SAFE, template.name(), style, tier, size, 0, false);
    }

    private static void add(List<Sample> result, Phase phase, String family, Cell cell, List<InputMember> members) {
        result.add(new Sample(result.size() + 1, family, cell, phase.pairs, members));
    }

    static int battleCount(Phase phase, int ratios) {
        return 72 * phase.pairs * 2 * ratios;
    }

    static int replayCount(Phase phase, int ratios) {
        return (phase == Phase.SCREEN ? 12 : 36) * 2 * ratios;
    }

    static Map<Integer, List<Seed>> plans(Phase phase, List<Sample> samples) {
        final var result = new TreeMap<Integer, List<Seed>>();
        for (final var sample : samples) {
            final var seeds = new ArrayList<Seed>();
            for (int pair = 0; pair < phase.pairs; pair++) {
                seeds.add(new Seed(pair, phase.generationRoot + sample.caseId() * 1000L + pair,
                    phase.battleRoot + sample.caseId() * 1000L + pair, sample.cell().size()));
            }
            result.put(sample.caseId(), List.copyOf(seeds));
        }
        return result;
    }

    static boolean replaySelected(Phase phase, Sample sample, int pair) {
        return pair == 0 && (sample.family().equals("A_MAIN") && sample.cell().tier() == Tier.MID
            || phase == Phase.CONFIRM && (sample.cell().style().equals("COMMAND_CUIRASS")
                || sample.family().equals("D_DEFAULTS")));
    }

    static List<String> ratios(Phase phase, Map<String, Object> selection) {
        if (phase == Phase.SCREEN) {
            require(selection.isEmpty(), "Screening cannot have a selection");
            return SCREEN_RATIOS;
        }
        final var chosen = selection.get("selectedRatio");
        require(chosen instanceof String && SCREEN_RATIOS.contains(chosen)
            && !BASELINE_RATIO.equals(chosen), "Selected ratio must be a non-baseline preregistered candidate");
        return List.of(BASELINE_RATIO, (String) chosen);
    }

    private static Map<String, Object> selection(Path path) throws Exception {
        final Map<String, Object> result = PveAdaptationStudy.JSON.readValue(path.toFile(), LinkedHashMap.class);
        final var screen = Path.of((String) result.get("screenRunPath"));
        for (final var link : Map.of("screenProtocolSha256", "protocol.json", "screenRawSha256", "battles.jsonl.gz",
            "screenSummarySha256", "summary.json").entrySet()) {
            require(sha256(screen.resolve(link.getValue())).equals(result.get(link.getKey())),
                "Selection does not bind screening " + link.getValue());
        }
        ratios(Phase.CONFIRM, result);
        return result;
    }

    private static PreparedState prepare(Sample sample, Seed seed, Generator generator) {
        return RandomUtils.withSeed(seed.generationSeed(), () -> new PreparedState(
            Step192AnomalyFormationStudy.prepare(sample, generator, PveAdaptationStudy.FORMULA),
            RandomUtils.getWithMax(1_000_000)));
    }

    private static Map<String, Object> generationSnapshot(PreparedState state) {
        final var prepared = state.prepared;
        return Map.of("members", prepared.members(), "players", Step192AnomalyFormationStudy.basis(prepared.players()),
            "enemies", Step192AnomalyFormationStudy.basis(prepared.generated().enemies()),
            "calibration", prepared.generated().calibration(), "diagnostics", prepared.generated().diagnostics(),
            "postGenerationRandomDraw", state.nextDraw);
    }

    private static void preflight(Phase phase, List<Sample> samples, Map<Integer, List<Seed>> plans,
                                  Candidates candidates, Map<String, Object> selection) throws Exception {
        int checks = 0;
        int productionChecks = 0;
        for (final var sample : samples) {
            for (final var seed : plans.get(sample.caseId())) {
                Object playerBasis = null;
                for (final var ratio : candidates.ratios) {
                    final var state = prepare(sample, seed, candidates.generator(ratio));
                    final var prepared = state.prepared;
                    require(prepared.generated().enemies().size() == sample.cell().size(), "Unexpected enemy count");
                    final var calibration = prepared.generated().calibration();
                    final double expected = prepared.players().stream().mapToDouble(BattlePersonage::legacyPower).sum()
                        * Double.parseDouble(ratio);
                    require(Double.doubleToLongBits(expected) == Double.doubleToLongBits(calibration.targetPower()),
                        "Candidate does not use its declared legacy-power ratio " + ratio);
                    final var players = Step192AnomalyFormationStudy.basis(prepared.players());
                    if (playerBasis == null) {
                        playerBasis = players;
                    } else {
                        requireSame(playerBasis, players, "player basis across ratios");
                    }
                    if (phase == Phase.CONFIRM && ratio.equals(selection.get("selectedRatio"))) {
                        requireSame(generationSnapshot(state), generationSnapshot(prepare(sample, seed,
                            Step192AnomalyFormationStudy.currentGenerator())), "actual production vs isolated candidate");
                        ++productionChecks;
                    }
                    ++checks;
                }
            }
        }
        require(checks == 72 * phase.pairs * candidates.ratios.size(), "Wrong preflight count");
        require(productionChecks == (phase == Phase.CONFIRM ? 72 * phase.pairs : 0), "Wrong actual-production check count");
        System.out.println("preflight phase=" + phase + " generations=" + checks
            + " productionCandidateFullSnapshotComparisons=" + productionChecks + "; no battles executed");
    }

    private static List<Map<String, Object>> runPair(Phase phase, Sample sample, Seed seed, Candidates candidates,
                                                    Map<String, Object> selection, BufferedWriter raw) throws Exception {
        final var rows = new ArrayList<Map<String, Object>>();
        for (final var ratio : candidates.ratios) {
            Object first = null;
            for (final boolean reverse : List.of(false, true)) {
                final var generator = phase == Phase.CONFIRM && ratio.equals(selection.get("selectedRatio"))
                    ? Step192AnomalyFormationStudy.currentGenerator() : candidates.generator(ratio);
                final var state = prepare(sample, seed, generator);
                final var snapshot = generationSnapshot(state);
                if (first == null) {
                    first = snapshot;
                } else {
                    requireSame(first, snapshot, "mirrored full generation");
                }
                final var row = runBattle(phase, sample, seed, ratio, reverse, state);
                raw.write(PveAdaptationStudy.JSON.writeValueAsString(Step192AnomalyFormationStudy.canonicalTree(row)));
                raw.newLine();
                rows.add(row);
            }
        }
        return rows;
    }

    private static Map<String, Object> runBattle(Phase phase, Sample sample, Seed seed, String ratio,
                                                boolean reverse, PreparedState state) throws Exception {
        final var prepared = state.prepared;
        final var players = prepared.players();
        final var enemies = prepared.generated().enemies();
        final var row = new LinkedHashMap<String, Object>();
        row.put("caseId", sample.caseId());
        row.put("logicalCaseId", sample.caseId());
        row.put("family", sample.family());
        row.put("cell", sample.cell());
        row.put("phase", phase);
        row.put("ratio", ratio);
        row.put("pair", seed.pair());
        row.put("generationSeed", seed.generationSeed());
        row.put("battleSeed", seed.battleSeed());
        row.put("reverse", reverse);
        row.put("enemyCount", enemies.size());
        row.put("postGenerationRandomDraw", state.nextDraw);
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
        row.put("playerBasis", Step192AnomalyFormationStudy.basis(players));
        row.put("enemyBasis", Step192AnomalyFormationStudy.basis(enemies));
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
            row.put("moves", result.actionLog().events().stream().filter(BattleEvent.MovedTowardEnemy.class::isInstance).count());
            row.put("forcedMoves", result.actionLog().events().stream().filter(BattleEvent.PersonageForcedMove.class::isInstance).count());
            row.put("actionSha256", digest(result.actionLog()));
            row.put("traceSha256", digest(result.actionLog().traceEvents()));
            final var counters = skillCounters(result.actionLog().events(), players, enemies);
            row.put("skillEvents", counters.events);
            row.put("skillEventsByKind", counters.byKind);
            row.put("enemyActionCounts", enemyActions(result.actionLog(), enemies));
            if (replaySelected(phase, sample, seed.pair())) {
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

    private static Map<String, Object> summarize(Sample sample, String ratio, List<Map<String, Object>> all) {
        final var rows = all.stream().filter(row -> row.get("ratio").equals(ratio)).toList();
        final var outcomes = new TreeMap<String, Integer>();
        final var byOrder = new TreeMap<String, Map<String, Integer>>();
        final var statuses = new TreeMap<String, Integer>();
        final var metrics = new TreeMap<String, List<Double>>();
        final var skills = new TreeMap<String, Long>();
        final var kinds = new TreeMap<String, Long>();
        final var byCount = new TreeMap<Integer, Map<String, Integer>>();
        final var pairScores = pairScores(sample, ratio, all);
        final var errors = new ArrayList<Double>();
        for (final var row : rows) {
            final var outcome = (String) row.get("outcome");
            outcomes.merge(outcome, 1, Integer::sum);
            byOrder.computeIfAbsent((Boolean) row.get("reverse") ? "PLAYERS_FIRST" : "ENEMIES_FIRST",
                unused -> new TreeMap<>()).merge(outcome, 1, Integer::sum);
            byCount.computeIfAbsent((Integer) row.get("enemyCount"), unused -> new TreeMap<>()).merge(outcome, 1, Integer::sum);
            final var calibration = (PveCharacteristicScaling.Result) row.get("calibration");
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
        result.put("ratio", ratio);
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
        result.put("pairedScore", Step192AnomalyFormationStudy.distribution(completeScores));
        if (!completeScores.isEmpty()) {
            result.put("pairedScore95Hoeffding", confidence(completeScores, false));
        }
        final var distributions = new TreeMap<String, Object>();
        metrics.forEach((key, values) -> distributions.put(key, Step192AnomalyFormationStudy.distribution(values)));
        result.put("completedBattleMetrics", distributions);
        result.put("calibrationAbsoluteRelativeError", Step192AnomalyFormationStudy.distribution(errors));
        return result;
    }

    private static List<Double> pairScores(Sample sample, String ratio, List<Map<String, Object>> rows) {
        final var scores = new ArrayList<Double>();
        for (int pair = 0; pair < sample.pairs(); pair++) {
            final int index = pair;
            final var mirrors = rows.stream().filter(row -> row.get("ratio").equals(ratio)
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

    private static Map<String, Object> comparison(Sample sample, String ratio, List<Map<String, Object>> rows) {
        final var before = pairScores(sample, BASELINE_RATIO, rows);
        final var after = pairScores(sample, ratio, rows);
        final var deltas = new ArrayList<Double>();
        for (int pair = 0; pair < before.size(); pair++) {
            deltas.add(before.get(pair) == null || after.get(pair) == null ? null : after.get(pair) - before.get(pair));
        }
        final var complete = deltas.stream().filter(java.util.Objects::nonNull).toList();
        final var result = new LinkedHashMap<String, Object>();
        result.put("logicalCaseId", sample.caseId());
        result.put("family", sample.family());
        result.put("cell", sample.cell());
        result.put("baselineRatio", BASELINE_RATIO);
        result.put("currentRatio", ratio);
        result.put("pairedDeltas", deltas);
        result.put("distribution", Step192AnomalyFormationStudy.distribution(complete));
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

    private static Map<String, Object> protocol(Phase phase, List<Sample> samples, Map<Integer, List<Seed>> plans,
                                               Candidates candidates, Path selectionPath, Map<String, Object> selection)
        throws Exception {
        final var result = new LinkedHashMap<String, Object>();
        result.put("protocolVersion", "STEP_19_3_DIFFICULTY_2026_11_17_V1");
        result.put("purpose", "ANOMALY_DIFFICULTY_" + phase);
        result.put("phase", phase);
        result.put("logicalCaseCount", samples.size());
        result.put("caseCount", samples.size() * candidates.ratios.size());
        result.put("attemptedBattles", battleCount(phase, candidates.ratios.size()));
        result.put("independentLogicalPairs", samples.size() * phase.pairs);
        result.put("preBattleGenerationCount", samples.size() * phase.pairs * candidates.ratios.size());
        result.put("productionCandidateFullSnapshotComparisons", phase == Phase.CONFIRM ? samples.size() * phase.pairs : 0);
        result.put("ratios", candidates.ratios);
        result.put("pairsPerCase", phase.pairs);
        result.put("mode", Mode.ANOMALY_SAFE);
        result.put("serviceCaveat", "One shared generator/Battle path; actual SAFE/FALLBACK service routes tested separately");
        result.put("metric", "LEGACY_SUM_V1");
        result.put("catalog", EquipmentCatalogVersion.SCALING_V5);
        result.put("playerSkills", PveAdaptationStudy.FORMULA);
        result.put("npcSkills", PveAdaptationStudy.FORMULA);
        result.put("tempo", PveAdaptationStudy.TEMPO);
        result.put("tempoDamageMultiplier", PveAdaptationStudy.TEMPO.damageMultiplier());
        result.put("roundLimit", ROUND_LIMIT);
        result.put("generationRoot", phase.generationRoot);
        result.put("battleRoot", phase.battleRoot);
        result.put("seedPolicy", "root + caseId*1000 + pair; shared seeds across ratios and mirrored orders; "
            + "coefficient may change search evaluation count and subsequent generation random draw positions");
        result.put("inferencePolicy", "Mirrors and ratio versions are dependent; logical pair indices are the inference unit");
        result.put("selectionPolicy", "Among A_MAIN aggregate WIN fractions in [0.75,0.90], nearest 0.80; "
            + "ties nearest 0.92, then lower ratio. Freeze choice before independent-seed confirmation");
        result.put("acceptancePolicy", "A_MAIN aggregate 75-90 percent; target near 80 percent; N1/N2 excluded from balance; "
            + "not a per-build 50-percent requirement; direction/default risks reported, not silently pooled into A_MAIN");
        result.put("durationPolicy", "Completed battles only; rounds/actions/turns are not seconds");
        result.put("timeoutPolicy", "TIMEOUT is not LOSS; incomplete pairs excluded from paired scores");
        result.put("skillEventPolicy", "Event counts by source side, skill and kind, not unique activations");
        result.put("canonicalizationPolicy", "Only slots and availableOnSlots arrays are semantic sets");
        result.put("candidateRootPath", candidates.root.toString());
        result.put("candidateMetadata", PveAdaptationStudy.JSON.readTree(candidates.root.resolve("metadata.json").toFile()));
        result.put("candidateMetadataSha256", sha256(candidates.root.resolve("metadata.json")));
        result.put("candidateClassSha256ByRatio", candidates.classHashes());
        result.put("currentClassSha256ByPath", currentClassHashes());
        result.put("publicSourceSha256", sourceHashes());
        if (phase == Phase.CONFIRM) {
            result.put("selection", selection);
            result.put("selectionJsonPath", selectionPath.toString());
            result.put("selectionJsonSha256", sha256(selectionPath));
        }
        result.put("replayPairs", samples.stream().flatMap(sample -> plans.get(sample.caseId()).stream()
            .filter(seed -> replaySelected(phase, sample, seed.pair())).map(seed -> Map.of("caseId", sample.caseId(),
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
            try (var walk = Files.walk(Path.of(root))) {
                paths.addAll(walk.filter(Files::isRegularFile).toList());
            }
        }
        for (final var path : List.of("game/src/test/java/ru/homyakin/seeker/game/battle/Step193AnomalyDifficultyStudy.java",
            "game/src/test/java/ru/homyakin/seeker/game/battle/Step193AnomalyDifficultyStudyTest.java",
            "game/src/test/java/ru/homyakin/seeker/game/battle/Step192AnomalyFormationStudy.java",
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
            EVIDENCE + "protocol.md", EVIDENCE + "validate-study.mjs", EVIDENCE + "prepare-candidates.mjs",
            EVIDENCE + "candidate-archive.json")) {
            paths.add(Path.of(path));
        }
        final var hashes = new TreeMap<String, String>();
        for (final var path : paths.stream().distinct().sorted().toList()) {
            hashes.put(path.toString(), sha256(path));
        }
        return hashes;
    }

    private static Map<String, String> classHashes(Path root) throws Exception {
        final var result = new TreeMap<String, String>();
        final var main = Path.of(GENERATOR.replace('.', '/') + ".class");
        final var parent = root.resolve(main).getParent();
        final var stem = main.getFileName().toString().replace(".class", "");
        try (var files = Files.list(parent)) {
            for (final var file : files.filter(Files::isRegularFile).toList()) {
                final var name = file.getFileName().toString();
                if (name.equals(stem + ".class") || name.startsWith(stem + "$") && name.endsWith(".class")) {
                    result.put(root.relativize(file).toString(), sha256(file));
                }
            }
        }
        require(result.containsKey(main.toString()) && result.containsKey(GENERATOR.replace('.', '/') + "$1.class"),
            "Incomplete isolated generator family");
        return result;
    }

    private static Map<String, String> currentClassHashes() throws Exception {
        final var result = new TreeMap<String, String>();
        for (final var suffix : List.of("", "$1")) {
            final var path = (GENERATOR + suffix).replace('.', '/') + ".class";
            try (var input = Step193AnomalyDifficultyStudy.class.getClassLoader().getResourceAsStream(path)) {
                require(input != null, "Missing production generator family class");
                result.put(path, bytesHash(input.readAllBytes()));
            }
        }
        return result;
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
            if (playerIds.contains(observation.source)) {
                side = "PLAYER";
            } else if (enemyIds.contains(observation.source)) {
                side = "ENEMY";
            } else {
                throw new IllegalStateException("Unknown skill event source");
            }
            final var key = side + ":" + observation.skill;
            counts.merge(key, 1L, Long::sum);
            kinds.merge(key + ":" + observation.kind, 1L, Long::sum);
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

    private static void requireSame(Object first, Object second, String label) {
        require(Step192AnomalyFormationStudy.canonicalTree(first).equals(Step192AnomalyFormationStudy.canonicalTree(second)),
            "Different " + label);
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
        Files.writeString(path, PveAdaptationStudy.JSON.writerWithDefaultPrettyPrinter().writeValueAsString(
            Step192AnomalyFormationStudy.canonicalTree(value)), StandardOpenOption.CREATE_NEW);
    }

    private static String digest(Object value) throws Exception {
        return bytesHash(PveAdaptationStudy.JSON.writeValueAsBytes(Step192AnomalyFormationStudy.canonicalTree(value)));
    }

    private static String bytesHash(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static String sha256(Path path) throws Exception {
        return bytesHash(Files.readAllBytes(path));
    }

    enum Phase {
        SCREEN(20, 20_261_203_000_000L, 30_261_203_000_000L),
        CONFIRM(80, 20_261_204_000_000L, 30_261_204_000_000L);

        final int pairs;
        final long generationRoot;
        final long battleRoot;

        Phase(int pairs, long generationRoot, long battleRoot) {
            this.pairs = pairs;
            this.generationRoot = generationRoot;
            this.battleRoot = battleRoot;
        }
    }

    private record PreparedState(PveAdaptationStudy.Prepared prepared, int nextDraw) { }
    private record SkillObservation(UUID source, ActiveEnum skill, String kind) { }
    private record SkillCounters(Map<String, Long> events, Map<String, Long> byKind) { }

    private static final class Candidates implements AutoCloseable {
        private final Path root;
        private final List<String> ratios;
        private final Map<String, Step192AnomalyFormationStudy.BaselineLoader> loaders = new LinkedHashMap<>();
        private final Map<String, Generator> generators = new LinkedHashMap<>();

        private Candidates(Path root, List<String> ratios) throws Exception {
            this.root = root;
            this.ratios = ratios;
            try {
                for (final var ratio : ratios) {
                    final var loader = new Step192AnomalyFormationStudy.BaselineLoader(
                        root.resolve("ratio-" + ratio).toUri().toURL());
                    loaders.put(ratio, loader);
                    generators.put(ratio, Step192AnomalyFormationStudy.baselineGenerator(loader));
                }
            } catch (Exception failure) {
                close();
                throw failure;
            }
        }

        private Generator generator(String ratio) {
            return generators.get(ratio);
        }

        private Map<String, Map<String, String>> classHashes() throws Exception {
            final var result = new TreeMap<String, Map<String, String>>();
            for (final var ratio : ratios) {
                result.put(ratio, Step193AnomalyDifficultyStudy.classHashes(root.resolve("ratio-" + ratio)));
            }
            return result;
        }

        @Override
        public void close() throws Exception {
            for (final var loader : loaders.values()) {
                loader.close();
            }
        }
    }
}
