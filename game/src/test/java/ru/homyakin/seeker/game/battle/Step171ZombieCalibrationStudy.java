package ru.homyakin.seeker.game.battle;

import com.fasterxml.jackson.databind.JsonNode;
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
import ru.homyakin.seeker.game.battle.Step123ReferenceTeams.Tier;
import ru.homyakin.seeker.game.battle.Step123BattleRunner.Gear;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.targeting.TargetingTactic;
import ru.homyakin.seeker.game.event.pve.PveCharacteristicScaling;
import ru.homyakin.seeker.game.event.raid.generator.ZombieHordeGenerator;
import ru.homyakin.seeker.game.event.raid.models.RaidType;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogVersion;
import ru.homyakin.seeker.utils.RandomUtils;

/** Bounded exploratory zombie correction, then a separate production-path holdout with fresh seeds. */
public final class Step171ZombieCalibrationStudy {
    static final String METRIC = "EQUIPMENT_POWER_SUM_V1";
    static final List<Double> CANDIDATES = List.of(1.0, 0.8, 0.65, 0.5);
    private static final List<Tier> TIERS = List.of(Tier.BEGINNER, Tier.MID, Tier.STRONG);

    private Step171ZombieCalibrationStudy() {
    }

    /** Every plan/run phase writes new outputs; replay-final requires a distinct new directory. */
    public static void main(String[] args) throws Exception {
        if ((args.length != 2 || !List.of("plan-screen", "run-screen", "plan-final", "run-final").contains(args[0]))
            && (args.length != 3 || !args[0].equals("replay-final"))) {
            throw new IllegalArgumentException("Expected plan-screen|run-screen|plan-final|run-final DIR"
                + ", or replay-final ORIGINAL_DIR NEW_DIR");
        }
        final var phase = args[0].endsWith("screen") ? Phase.SCREEN : Phase.FINAL;
        final var output = Path.of(args[1]);
        final var cases = cases(phase);
        final var protocol = protocol(cases);
        final var protocolPath = output.resolve("protocol.json");
        if (args[0].startsWith("plan-")) {
            Files.createDirectory(output);
            writeJson(protocolPath, protocol);
            System.out.println("planned phase=" + phase + " cells=" + cases.size() + " battles=" + battleCount(cases)
                + " protocolSha256=" + sha256(protocolPath));
            return;
        }
        if (!canonicalTree(PveAdaptationStudy.JSON.readTree(protocolPath.toFile())).equals(canonicalTree(protocol))) {
            throw new IllegalStateException("The pre-battle protocol, correction, inputs or public sources changed");
        }
        if (args[0].equals("replay-final")) {
            final var replay = Path.of(args[2]);
            Files.createDirectory(replay);
            try (var raw = writer(replay.resolve("battles.jsonl.gz"))) {
                for (final var sample : cases) {
                    if (replayCaseIds(cases).contains(sample.caseId())) {
                        runCase(sample, raw, 1);
                    }
                }
            }
            System.out.println("replayed cells=16 battles=32");
            return;
        }
        final var summaries = new ArrayList<Map<String, Object>>();
        final var rawPath = output.resolve("battles.jsonl.gz");
        try (var raw = writer(rawPath)) {
            for (final var sample : cases) {
                summaries.add(runCase(sample, raw, sample.pairs()));
                if (sample.caseId() % 10 == 0 || sample.caseId() == cases.size()) {
                    raw.flush();
                    System.out.println("completed phase=" + phase + " cells=" + sample.caseId() + "/" + cases.size());
                }
            }
        }
        final var summary = new LinkedHashMap<String, Object>();
        summary.put("purpose", phase == Phase.SCREEN ? "BOUNDED_EXPLORATORY_CORRECTION_NOT_HOLDOUT"
            : "FIXED_PRODUCTION_PATH_HOLDOUT_NO_RETUNING");
        summary.put("phase", phase);
        summary.put("protocolSha256", sha256(protocolPath));
        summary.put("rawSha256", sha256(rawPath));
        summary.put("caseCount", cases.size());
        summary.put("attemptedBattles", battleCount(cases));
        summary.put("cases", summaries);
        summary.put("comparisons", comparisons(cases, summaries));
        if (phase == Phase.SCREEN) {
            summary.put("exploratorySelection", selection(cases, summaries));
        } else {
            summary.put("finalAcceptance", finalAcceptance(cases, summaries));
        }
        writeJson(output.resolve("summary.json"), summary);
        System.out.println("complete phase=" + phase + " cells=" + cases.size() + " battles=" + battleCount(cases));
    }

    static List<Sample> cases(Phase phase) {
        final double production = expectedProductionCorrection();
        final var result = new ArrayList<Sample>();
        int seedGroup = 0;
        if (phase == Phase.SCREEN) {
            for (final var style : List.of("MAIN", "RITUAL", "BLUNT_LEATHER")) {
                for (final var tier : TIERS) {
                    final var sizes = style.equals("MAIN") ? List.of(1, 3, 7, 10, 11, 13, 14, 15, 30) : List.of(3, 13);
                    for (final int size : sizes) {
                        ++seedGroup;
                        final var cell = new Cell(Mode.RAID, RaidType.ZOMBIE_HORDE.name(), style, tier, size, 10, false);
                        final List<InputMember> members;
                        if (style.equals("BLUNT_LEATHER")) {
                            final var build = Step12RepresentativeBuilds.hand(style, List.of("mace", "club"), "LEATHER",
                                Position.FRONT, TargetingTactic.THREAT);
                            final var gear = new Gear(build, EquipmentCatalogVersion.SCALING_V5);
                            members = java.util.stream.IntStream.range(0, size).mapToObj(index ->
                                new InputMember(build.code(), build.position(), build.tactic(), gear.items(tier.level())))
                                .toList();
                        } else {
                            members = PveAdaptationStudy.inputFor(cell);
                        }
                        for (final double correction : CANDIDATES) {
                            result.add(new Sample(result.size() + 1, style.equals("MAIN") ? "S_MAIN" : "S_DIAGNOSTIC",
                                correction == 1 ? "BASE" : "CORRECTION_" + correction,
                                "EXPLORATORY_REQUESTED_BONUS_OVERRIDE_NOT_FAKE_LEVEL", seedGroup, 10, cell, members,
                                phase, correction, production));
                        }
                    }
                }
            }
        } else {
            final var groups = new TreeMap<Integer, Integer>();
            for (final var old : Step1618RaidValidationStudy.cases()) {
                if (!old.cell().encounter().equals(RaidType.ZOMBIE_HORDE.name())) {
                    continue;
                }
                groups.putIfAbsent(old.seedGroup(), groups.size() + 1);
                final int pairs = old.family().equals("A_MAIN") && old.cell().raidLevel() == 10
                    && List.of(3, 13).contains(old.cell().size()) ? 100 : old.pairs();
                result.add(new Sample(result.size() + 1, old.family(), old.variant(), old.change(),
                    groups.get(old.seedGroup()), pairs, old.cell(), old.members(), phase, production, production));
            }
            seedGroup = groups.size();
            for (final var tier : TIERS) {
                for (final int size : List.of(1, 3, 7, 13, 30)) {
                    for (final int level : List.of(5, 15, 20)) {
                        final var cell = new Cell(Mode.RAID, RaidType.ZOMBIE_HORDE.name(), "MAIN", tier, size, level, false);
                        result.add(new Sample(result.size() + 1, "H_INTERMEDIATE", "BASE", "PREDECLARED_INTERMEDIATE_LEVEL",
                            ++seedGroup, 10, cell, PveAdaptationStudy.inputFor(cell), phase, production, production));
                    }
                }
            }
        }
        if (result.size() != (phase == Phase.SCREEN ? 156 : 401)
            || battleCount(result) != (phase == Phase.SCREEN ? 3120 : 8300)
            || seedGroup != (phase == Phase.SCREEN ? 39 : 329)) {
            throw new IllegalStateException("The fixed step 17.1 matrix changed");
        }
        return List.copyOf(result);
    }

    static List<Integer> replayCaseIds(List<Sample> cases) {
        final var result = cases.stream().filter(Step171ZombieCalibrationStudy::replaySelected)
            .map(Sample::caseId).toList();
        if (result.size() != (cases.getFirst().phase() == Phase.SCREEN ? 0 : 16)) {
            throw new IllegalStateException("The fixed holdout replay selection changed");
        }
        return result;
    }

    private static boolean replaySelected(Sample sample) {
        if (sample.phase() == Phase.SCREEN) {
            return false;
        }
        final var cell = sample.cell();
        if (sample.family().equals("A_MAIN") && cell.tier() == Tier.MID) {
            return cell.raidLevel() == 25 && List.of(1, 3, 10, 11, 13, 14, 30).contains(cell.size())
                || cell.raidLevel() == 10 && List.of(3, 7, 13, 30).contains(cell.size());
        }
        if (cell.tier() != Tier.BEGINNER) {
            return false;
        }
        return switch (sample.family()) {
            case "B_TYPES" -> cell.style().equals("SLASH_LEATHER") && cell.size() == 1 && cell.raidLevel() == 1;
            case "D_EFFECTS" -> cell.size() == 3 && cell.raidLevel() == 1;
            case "E_CAUSAL" -> cell.size() == 3 && cell.raidLevel() == 10 && sample.variant().equals("BASE");
            case "F_DEFAULTS" -> cell.size() == 1 && cell.raidLevel() == 1;
            case "G_WORKABLE" -> cell.style().equals("RITUAL") && cell.size() == 1 && cell.raidLevel() == 1;
            default -> false;
        };
    }

    static double expectedProductionCorrection() {
        final double value = Double.parseDouble(System.getProperty("zombie.study.productionCorrection", "1"));
        if (!Double.isFinite(value) || value <= 0) {
            throw new IllegalArgumentException("Expected production correction must be positive and finite");
        }
        return value;
    }

    static double baseLevelBonus(Sample sample) {
        return 1.0 + ((sample.cell().raidLevel() - 10) * 0.06 + 0.14);
    }

    static double requestedPowerBonus(Sample sample) {
        return sample.phase() == Phase.SCREEN
            ? baseLevelBonus(sample) * sample.correction() / sample.productionCorrection() : baseLevelBonus(sample);
    }

    static long generationRoot(Phase phase) {
        return phase == Phase.SCREEN ? 20_261_120_000_000L : 20_261_121_000_000L;
    }

    static long battleRoot(Phase phase) {
        return phase == Phase.SCREEN ? 30_261_120_000_000L : 30_261_121_000_000L;
    }

    private static PveAdaptationStudy.Prepared prepare(Sample sample) {
        final PveAdaptationStudy.Prepared result;
        if (sample.phase() == Phase.FINAL) {
            result = PveAdaptationStudy.prepareMembers(sample.cell(), sample.members());
        } else {
            final var players = new ArrayList<BattlePersonage>();
            final var members = new ArrayList<PveAdaptationStudy.Member>();
            for (final var member : sample.members()) {
                final var actor = BattlePersonage.forCombat(member.items(), member.position(),
                    combatEffects(sample.cell()), Optional.of(member.build()), PveAdaptationStudy.FORMULA);
                actor.setTargetingTactic(member.tactic());
                players.add(actor);
                members.add(new PveAdaptationStudy.Member(member.build(), member.position(), member.tactic().name(),
                    member.items(), member.items()));
            }
            final var generated = new ZombieHordeGenerator().generateCalibrated(
                players, requestedPowerBonus(sample), PveAdaptationStudy.FORMULA);
            result = new PveAdaptationStudy.Prepared(List.copyOf(players), generated, List.copyOf(members));
        }
        final var diagnostics = result.generated().diagnostics().orElseThrow();
        if (Double.compare(diagnostics.parameters().get("encounterCorrection"), sample.productionCorrection()) != 0
            || Double.compare(diagnostics.parameters().get("powerBonus"), requestedPowerBonus(sample)) != 0) {
            throw new IllegalStateException("Actual production correction or requested power bonus differs from protocol");
        }
        for (final var actor : java.util.stream.Stream.concat(result.players().stream(),
            result.generated().enemies().stream()).toList()) {
            if (!actor.usesScalingCombatRules() || actor.skillSnapshots().stream()
                .anyMatch(skill -> skill.formulaVersion() != PveAdaptationStudy.FORMULA)) {
                throw new IllegalStateException("A trial participant is not V3");
            }
        }
        return result;
    }

    static Map<String, Object> selection(List<Sample> cases, List<Map<String, Object>> summaries) {
        final var candidates = new ArrayList<Map<String, Object>>();
        Double selected = null;
        for (final double correction : CANDIDATES) {
            int wins = 0;
            int timeouts = 0;
            final var tierWins = new TreeMap<String, Integer>();
            final var diagnosticWins = new TreeMap<String, Integer>();
            for (final var sample : cases) {
                if (Double.compare(sample.correction(), correction) != 0) {
                    continue;
                }
                final var outcomes = (Map<?, ?>) summaries.get(sample.caseId() - 1).get("outcomes");
                final int actualWins = count(outcomes, "WIN");
                timeouts += count(outcomes, "TIMEOUT");
                if (sample.family().equals("S_MAIN")) {
                    wins += actualWins;
                    tierWins.merge(sample.cell().tier().name(), actualWins, Integer::sum);
                } else {
                    diagnosticWins.merge(sample.cell().style(), actualWins, Integer::sum);
                }
            }
            final long passingTiers = TIERS.stream().filter(tier -> tierWins.getOrDefault(tier.name(), 0) >= 36).count();
            final boolean passed = wins >= 135 && passingTiers >= 2 && timeouts == 0;
            if (selected == null && passed) {
                selected = correction;
            }
            final var row = new LinkedHashMap<String, Object>();
            row.put("correction", correction);
            row.put("mainWins", wins);
            row.put("mainBattles", 540);
            row.put("mainWinsByTier", tierWins);
            row.put("mainBattlesPerTier", 180);
            row.put("diagnosticWinsByStyle", diagnosticWins);
            row.put("diagnosticBattlesPerStyle", 120);
            row.put("timeoutsAllStyles", timeouts);
            row.put("passesPredeclaredRule", passed);
            candidates.add(row);
        }
        final var result = new LinkedHashMap<String, Object>();
        result.put("purpose", "DESCRIPTIVE_SCREEN_ONLY_NOT_HOLDOUT_OR_AUTOMATIC_RELEASE");
        result.put("candidates", candidates);
        result.put("selectedCorrection", selected);
        result.put("status", selected == null ? "STOP_NO_CANDIDATE_PASSES" : "LARGEST_PASSING_CANDIDATE");
        return result;
    }

    private static int count(Map<?, ?> outcomes, String key) {
        final var value = outcomes.get(key);
        return value == null ? 0 : ((Number) value).intValue();
    }

    static Map<String, Object> finalAcceptance(List<Sample> cases, List<Map<String, Object>> summaries) {
        final var tierRates = new TreeMap<String, List<Double>>();
        int timeouts = 0;
        int rawWins = 0;
        int rawBattles = 0;
        for (final var sample : cases) {
            final var outcomes = (Map<?, ?>) summaries.get(sample.caseId() - 1).get("outcomes");
            timeouts += count(outcomes, "TIMEOUT");
            if (sample.family().equals("A_MAIN") && sample.cell().raidLevel() == 10
                && List.of(1, 3, 7, 10, 11, 13, 14, 15, 30).contains(sample.cell().size())) {
                final int wins = count(outcomes, "WIN");
                final int attempts = sample.pairs() * 2;
                tierRates.computeIfAbsent(sample.cell().tier().name(), unused -> new ArrayList<>())
                    .add((double) wins / attempts);
                rawWins += wins;
                rawBattles += attempts;
            }
        }
        if (tierRates.size() != 3 || tierRates.values().stream().anyMatch(values -> values.size() != 9)) {
            throw new IllegalStateException("Holdout acceptance must use exactly the same 27 main contexts");
        }
        final var byTier = new TreeMap<String, Double>();
        tierRates.forEach((tier, rates) -> byTier.put(tier, rates.stream().mapToDouble(Double::doubleValue)
            .average().orElseThrow()));
        final double mean = byTier.values().stream().mapToDouble(Double::doubleValue).average().orElseThrow();
        final boolean passed = mean >= 0.25 && byTier.values().stream().filter(value -> value >= 0.20).count() >= 2
            && timeouts == 0;
        final var result = new LinkedHashMap<String, Object>();
        result.put("purpose", "FIXED_HOLDOUT_CONFIRMATION_NOT_ANOTHER_COEFFICIENT_SEARCH");
        result.put("selectedCorrection", cases.getFirst().productionCorrection());
        result.put("mainContextCount", 27);
        result.put("balancedMainWinFraction", mean);
        result.put("balancedTierWinFractions", byTier);
        result.put("rawMainWinsNotAcceptanceMetric", rawWins);
        result.put("rawMainBattlesNotAcceptanceMetric", rawBattles);
        result.put("timeoutsAllCases", timeouts);
        result.put("passesPredeclaredRule", passed);
        result.put("status", passed ? "CONFIRMED_ON_FRESH_SEEDS" : "STOP_HOLDOUT_NOT_CONFIRMED");
        return result;
    }

    private static Map<String, Object> protocol(List<Sample> cases) throws Exception {
        final var result = new LinkedHashMap<String, Object>();
        result.put("protocolVersion", "STEP_17_1_FIXED_2026_11_15_V1");
        result.put("phase", cases.getFirst().phase());
        result.put("expectedProductionCorrection", cases.getFirst().productionCorrection());
        result.put("productionCorrection", cases.getFirst().productionCorrection());
        result.put("correctionCandidates", cases.getFirst().phase() == Phase.SCREEN ? CANDIDATES : List.of());
        result.put("selectionPolicy", "Screen only: largest C, MAIN wins>=135/540, 2 tiers>=36/180, no TIMEOUT/error");
        result.put("finalAcceptancePolicy", "Final only: equal-weight mean of 27 MAIN L10 win fractions>=.25"
            + ", at least 2 tier means>=.20, no TIMEOUT/error; not a raw pooled percentage");
        result.put("metric", METRIC);
        result.put("roundLimit", 2000);
        result.put("skillEventPolicy", "Counts log events, not unique activations; source side plus separate event kind");
        result.put("canonicalizationPolicy", "Only slots and availableOnSlots arrays are semantic sets");
        result.put("catalog", EquipmentCatalogVersion.SCALING_V5);
        result.put("playerSkills", PveAdaptationStudy.FORMULA);
        result.put("npcSkills", PveAdaptationStudy.FORMULA);
        result.put("tempo", PveAdaptationStudy.TEMPO);
        result.put("tempoDamageMultiplier", PveAdaptationStudy.TEMPO.damageMultiplier());
        result.put("generationRoot", generationRoot(cases.getFirst().phase()));
        result.put("battleRoot", battleRoot(cases.getFirst().phase()));
        result.put("seedPolicy", "root + seedGroup * 1000 + pair; mirrors share seeds and use fresh actors");
        result.put("couplingCaveat", "Added or removed members change IDs and random draws despite shared roots");
        result.put("confidencePolicy", "Per-cell 95% Hoeffding of independent complete pair scores; not simultaneous");
        result.put("timeoutPolicy", "TIMEOUT is not LOSS; incomplete pairs omitted from paired scores");
        result.put("durationPolicy", "Round/action/turn distributions include completed battles only");
        result.put("scorePolicy", "WIN=1 DRAW=0.5 LOSS=0; pair score averages its two dependent mirrors");
        result.put("acceptancePolicy", "No 50/50 target, old-rate equality, universal ally benefit or -20pp rejection");
        result.put("caseCount", cases.size());
        result.put("attemptedBattles", battleCount(cases));
        result.put("replayCaseIds", replayCaseIds(cases));
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
                () -> prepare(sample));
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
                final var row = runWithMembers(sample, generationSeed, battleSeed(sample, pair), reverse,
                    pair == 0 && replaySelected(sample));
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
        result.put("phase", sample.phase());
        result.put("effectiveEncounterCorrection", sample.correction());
        result.put("expectedProductionCorrection", sample.productionCorrection());
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
                && !((sample.family().equals("E_CAUSAL") || sample.phase() == Phase.SCREEN)
                    && !sample.variant().equals("BASE"))) {
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

    static long generationSeed(Sample sample, int pair) {
        return generationRoot(sample.phase()) + sample.seedGroup() * 1000L + pair;
    }

    static long battleSeed(Sample sample, int pair) {
        return battleRoot(sample.phase()) + sample.seedGroup() * 1000L + pair;
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
        return Step1618RaidValidationStudy.canonicalTree(value);
    }

    private static Map<String, Object> runWithMembers(
        Sample sample, long generationSeed, long battleSeed, boolean reverse, boolean keepLogs
    ) throws Exception {
        final var cell = sample.cell();
        final var state = RandomUtils.withSeed(generationSeed, () -> {
            final var prepared = prepare(sample);
            return new PreparedState(prepared, RandomUtils.getWithMax(1_000_000));
        });
        final var prepared = state.prepared();
        final var players = prepared.players();
        final var enemies = prepared.generated().enemies();
        final var row = new LinkedHashMap<String, Object>();
        row.put("cell", cell);
        row.put("phase", sample.phase());
        row.put("baseLevelBonus", baseLevelBonus(sample));
        row.put("effectiveEncounterCorrection", sample.correction());
        row.put("expectedProductionCorrection", sample.productionCorrection());
        row.put("requestedPowerBonus", requestedPowerBonus(sample));
        row.put("postGenerationRandomDraw", state.nextDraw());
        row.put("playerGenerationGauges", players.stream().map(BattlePersonage::initiativeGauge).toList());
        row.put("enemyGenerationGauges", enemies.stream().map(BattlePersonage::initiativeGauge).toList());
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

    private record PreparedState(PveAdaptationStudy.Prepared prepared, int nextDraw) { }

    private record SkillObservation(java.util.UUID source, ActiveEnum skill, String kind) { }

    private record SkillCounters(Map<String, Long> events, Map<String, Long> byKind) { }

    private static Map<String, String> sourceHashes() throws Exception {
        final var old = PveAdaptationStudy.JSON.readTree(
            Path.of("documentation/raid-redesign-steps-16-18-evidence/run/protocol.json").toFile());
        final var paths = new ArrayList<String>();
        old.get("publicSourceSha256").fieldNames().forEachRemaining(paths::add);
        paths.addAll(List.of(
            "game/src/test/java/ru/homyakin/seeker/game/battle/Step171ZombieCalibrationStudy.java",
            "game/src/test/java/ru/homyakin/seeker/game/battle/Step171ZombieCalibrationStudyTest.java",
            "documentation/raid-redesign-step-17-1-evidence/protocol.md"
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
                  int seedGroup, int pairs, Cell cell, List<InputMember> members,
                  Phase phase, double correction, double productionCorrection) {
        Sample {
            members = List.copyOf(members);
            if (members.size() != cell.size()) {
                throw new IllegalArgumentException("Input member count differs from the declared cell");
            }
        }
    }

    enum Phase { SCREEN, FINAL }
}
