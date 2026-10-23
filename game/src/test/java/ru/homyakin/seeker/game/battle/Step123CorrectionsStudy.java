package ru.homyakin.seeker.game.battle;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;
import ru.homyakin.seeker.game.battle.Step123BattleRunner.Gear;
import ru.homyakin.seeker.game.battle.Step123ReferenceTeams.Specialization;
import ru.homyakin.seeker.game.battle.Step123ReferenceTeams.Tier;
import ru.homyakin.seeker.game.battle.Step12RepresentativeBattleRunner.Observation;
import ru.homyakin.seeker.game.battle.Step12RepresentativeBattleRunner.TeamMeasure;
import ru.homyakin.seeker.game.battle.Step12RepresentativeBuilds.Assignment;
import ru.homyakin.seeker.game.battle.Step12RepresentativeBuilds.Build;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.V3Build;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.targeting.TargetingTactic;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogVersion;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemRarity;

/** Fresh evidence for the movement and V5 catalog corrections; never activates a release. */
public final class Step123CorrectionsStudy {
    private static final int MAX_ROUNDS = 10_000;
    private static final JsonMapper JSON = JsonMapper.builder().addModule(new Jdk8Module()).build();

    private Step123CorrectionsStudy() {
    }

    static List<StudyCase> cases() {
        final var result = new ArrayList<StudyCase>();
        addWeapons(result);
        addMobile(result);
        addReferences(result);
        addPressureRear(result);
        addFrozenControls(result);
        result.sort(Comparator.comparing(StudyCase::code));
        if (result.stream().map(StudyCase::code).distinct().count() != result.size()) {
            throw new IllegalStateException("Duplicate correction case code");
        }
        return List.copyOf(result);
    }

    private static void addWeapons(List<StudyCase> result) {
        final var halberd = weapon("HALBERD", List.of("halberd"));
        for (final var catalog : List.of(EquipmentCatalogVersion.SCALING_V4, EquipmentCatalogVersion.SCALING_V5)) {
            addWeaponPair(result, halberd, weapon("CROSSBOW", List.of("crossbow")), catalog);
            addWeaponPair(result, halberd, weapon("STAFF_ORB", List.of("staff", "orb")), catalog);
        }
        addWeaponPair(result, halberd, weapon("TWO_HANDED_SWORD", List.of("two_handed_sword")),
            EquipmentCatalogVersion.SCALING_V5);
    }

    private static Build weapon(String code, List<String> hands) {
        return Step12RepresentativeBuilds.hand(code, hands, "LEATHER", Position.MID, TargetingTactic.THREAT);
    }

    private static void addWeaponPair(List<StudyCase> result, Build left, Build right, EquipmentCatalogVersion catalog) {
        for (final boolean anchored : List.of(false, true)) {
            final var group = "HALBERD_VS_" + right.code() + (anchored ? "_ANCHORED3" : "_DUEL");
            final var first = gear(anchored ? Step12RepresentativeMatrix.anchored(left) : List.of(left), catalog);
            final var second = gear(anchored ? Step12RepresentativeMatrix.anchored(right) : List.of(right), catalog);
            for (final int level : List.of(0, 3, 6)) {
                result.add(new StudyCase(group + "_" + catalog + "_L" + level, "WEAPON", "ISOLATED_COMMON",
                    "WEAPON_" + group, level, first, second, Set.of()));
            }
        }
    }

    private static void addMobile(List<StudyCase> result) {
        final var mobile = Step12RepresentativeBuilds.hand(
            "MOBILE_SPEAR_DIRK_LEATHER", List.of("spear", "dirk"), "LEATHER", Position.FRONT, TargetingTactic.THREAT
        ).withAssignments(Map.of(
            "boots", new Assignment(ActiveEnum.HIT_AND_RUN, ItemRarity.RARE),
            "leather_helm", new Assignment(ActiveEnum.HIT_AND_RUN, ItemRarity.RARE)
        ));
        final var support = Step123BattleStudy.historicalV3Build(V3Build.CROSSBOW_SUPPORT).withPosition(Position.MID);
        final var left = gear(List.of(mobile, support, support), EquipmentCatalogVersion.SCALING_V5);
        final var common = List.of(
            Step12RepresentativeRisks.ranked("G", ItemRarity.RARE),
            Step12RepresentativeRisks.ranked("C", ItemRarity.RARE),
            Step12RepresentativeRisks.ranked("C", ItemRarity.RARE)
        ).stream().map(build -> build.withAssignments(Map.of())).toList();
        for (final int level : List.of(0, 6, 10)) {
            for (final boolean disabled : List.of(false, true)) {
                result.add(new StudyCase("MOBILE_COMMON_FRONT_PRESSURE_L" + level + (disabled ? "_OFF" : "_ON"),
                    "MOBILE", "FIXED_FOUR_POINT_NICHE", "MOBILE_COMMON_FRONT_PRESSURE", level, left,
                    gear(common, EquipmentCatalogVersion.SCALING_V5),
                    disabled ? Set.of(ActiveEnum.HIT_AND_RUN) : Set.of()));
            }
        }
    }

    private static void addReferences(List<StudyCase> result) {
        for (final var tier : Tier.values()) {
            final var main = Step123ReferenceTeams.mainTeam(tier, 3);
            for (final var specialization : Specialization.values()) {
                final var specialized = Step123ReferenceTeams.specializedTeam(tier, specialization);
                final var group = "REFERENCE_MAIN3_VS_" + specialization + "_" + tier;
                result.add(new StudyCase(group, "REFERENCE", tier.name(), group, tier.level(),
                    main.members(), specialized.members(), Set.of()));
            }
            for (final int size : List.of(7, 15)) {
                final var larger = Step123ReferenceTeams.mainTeam(tier, size);
                final var group = "REFERENCE_MAIN" + size + "_MIRROR_" + tier;
                result.add(new StudyCase(group, "REFERENCE_MIRROR", tier.name(), group, tier.level(),
                    larger.members(), larger.members(), Set.of()));
            }
        }
    }

    private static void addPressureRear(List<StudyCase> result) {
        for (final var rarity : List.of(ItemRarity.RARE, ItemRarity.LEGENDARY)) {
            final var guard = Step12RepresentativeRisks.ranked("G", rarity);
            final var bruiser = Step12RepresentativeRisks.ranked("C", rarity);
            final var ranger = Step12RepresentativeRisks.ranked("D", rarity);
            for (final int size : List.of(3, 7, 15)) {
                final var left = gear(cycle(size, List.of(guard, bruiser, bruiser)), EquipmentCatalogVersion.SCALING_V5);
                final var right = gear(cycle(size, List.of(guard, ranger, ranger)), EquipmentCatalogVersion.SCALING_V5);
                final var group = "PRESSURE_REAR_N" + size + "_" + rarity;
                for (final int level : List.of(3, 6, 10)) {
                    result.add(new StudyCase(group + "_L" + level, "PRESSURE_REAR", "DIAGNOSTIC_" + rarity,
                        group, level, left, right, Set.of()));
                }
            }
        }
    }

    private static void addFrozenControls(List<StudyCase> result) {
        int count = 0;
        for (final var historical : Step123BattleStudy.cases()) {
            final boolean cloth = historical.family().equals("CLOTH")
                && (historical.code().startsWith("CLOTH_KNOCKBACK_BLUNT_LEATHER_OLD_L")
                    || historical.code().startsWith("CLOTH_KNOCKBACK_BLUNT_LEATHER_NEW_V4_L"));
            final boolean leader = historical.family().equals("CONTROL")
                && (historical.code().startsWith("CONTROL_RESPONSE_sword+dirk_sword+shield_L")
                    || historical.code().startsWith("CONTROL_RESPONSE_sword+dirk_COMMAND_NEW_LEATHER_L")
                    || historical.code().startsWith("CONTROL_PRESSURE_NO_SOFT_BACK_L"));
            if ((cloth || leader) && (historical.level() == 0 || historical.level() == 10)) {
                result.add(new StudyCase("FROZEN_CONTROL_" + historical.code(), "FROZEN_CONTROL", "HISTORICAL_INPUT_CONTROL",
                    "FROZEN_CONTROL_" + historical.seedGroup(), historical.level(),
                    gear(historical.left().stream().map(Gear::build).toList(), EquipmentCatalogVersion.SCALING_V5),
                    gear(historical.right().stream().map(Gear::build).toList(), EquipmentCatalogVersion.SCALING_V5),
                    historical.disabledLeft()));
                count++;
            }
        }
        if (count != 10) {
            throw new IllegalStateException("The ten explicitly selected historical controls must remain available");
        }
    }

    private static List<Build> cycle(int size, List<Build> values) {
        final var result = new ArrayList<Build>();
        for (int index = 0; index < size; index++) {
            result.add(values.get(index % values.size()));
        }
        return List.copyOf(result);
    }

    private static List<Gear> gear(List<Build> builds, EquipmentCatalogVersion catalog) {
        return builds.stream().map(build -> new Gear(build, catalog)).toList();
    }

    /** Arguments: a fresh output directory, root seed, number of paired seeds per condition. */
    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            throw new IllegalArgumentException("Usage: Step123CorrectionsStudy <outputDir> <rootSeed> <pairs>");
        }
        final var output = Path.of(args[0]).toAbsolutePath().normalize();
        final long rootSeed = Long.parseLong(args[1]);
        final int pairs = Integer.parseInt(args[2]);
        final int workers = Integer.getInteger("step123.corrections.workers", 4);
        final var filter = Pattern.compile(System.getProperty("step123.corrections.filter", ".*"));
        final var selected = cases().stream().filter(value -> filter.matcher(value.code()).find()).toList();
        if (pairs < 1 || workers < 1 || selected.isEmpty()) {
            throw new IllegalArgumentException("Pairs and workers must be positive, and the case filter must match");
        }
        Files.createDirectories(output);
        for (final var name : List.of("inputs.sha256", "cases.json", "battles.tsv.gz", "summary.tsv",
            "run.properties", "complete.properties")) {
            if (Files.exists(output.resolve(name))) {
                throw new IllegalArgumentException("Study output already exists: " + output.resolve(name));
            }
        }
        final var inputs = sourceInputs();
        Files.writeString(output.resolve("inputs.sha256"), inputs, StandardOpenOption.CREATE_NEW);
        Files.writeString(output.resolve("run.properties"), "rootSeed=" + rootSeed + "\npairs=" + pairs
            + "\nworkers=" + workers + "\nfilter=" + filter + "\ncases=" + selected.size()
            + "\nmaxRounds=" + MAX_ROUNDS + "\n", StandardOpenOption.CREATE_NEW);
        writeCases(output, selected, rootSeed, pairs);
        final var summaries = new ArrayList<String>();
        long violations = 0;
        long timeouts = 0;
        final long started = System.nanoTime();
        try (final var writer = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(
            Files.newOutputStream(output.resolve("battles.tsv.gz"), StandardOpenOption.CREATE_NEW)), StandardCharsets.UTF_8));
             final var executor = Executors.newFixedThreadPool(workers)) {
            writer.write("case\tfamily\ttier\tlevel\tpair\tseed\treverse\toutcome\trounds\t" + auditHeader()
                + "\tleftReactionDamage\trightReactionDamage\t" + metricHeader("left_") + "\t" + metricHeader("right_") + "\n");
            final var futures = new ArrayList<Future<CaseResult>>();
            for (final var value : selected) {
                futures.add(executor.submit(() -> run(value, rootSeed, pairs)));
            }
            for (int index = 0; index < futures.size(); index++) {
                final var result = futures.get(index).get();
                writer.write(result.raw());
                summaries.add(result.summary());
                violations += result.violations();
                timeouts += result.timeouts();
                if ((index + 1) % 10 == 0 || index + 1 == futures.size()) {
                    System.out.printf(Locale.ROOT, "Completed %d/%d; battles=%d; violations=%d; timeouts=%d; seconds=%.1f%n",
                        index + 1, selected.size(), (index + 1) * pairs * 2L, violations, timeouts,
                        (System.nanoTime() - started) / 1e9);
                }
            }
        }
        Files.writeString(output.resolve("summary.tsv"),
            "case\tfamily\ttier\tlevel\tsize\tpairs\twins\tlosses\tdraws\ttimeouts\tresolvedPairs\tscore\tlower\tupper"
                + "\tmeanRounds\tmaxRounds\t" + auditHeader() + "\tmeanLeftReactionDamage\tmeanRightReactionDamage\t"
                + metricHeader("meanLeft_") + "\t" + metricHeader("meanRight_") + "\n"
                + String.join("\n", summaries) + "\n", StandardOpenOption.CREATE_NEW);
        if (!inputs.equals(sourceInputs())) {
            throw new IllegalStateException("Source inputs changed during this study; no completion receipt written");
        }
        Files.writeString(output.resolve("complete.properties"), "rootSeed=" + rootSeed + "\npairs=" + pairs
            + "\ncases=" + selected.size() + "\nbattles=" + selected.size() * pairs * 2L
            + "\ntimeouts=" + timeouts + "\nforcedMoveViolations=" + violations
            + "\nmovementAuditPassed=" + (violations == 0) + "\n", StandardOpenOption.CREATE_NEW);
        if (violations != 0) {
            throw new IllegalStateException("Forced movement audit failed: " + violations + " movements exceeded one line");
        }
    }

    private static void writeCases(Path output, List<StudyCase> selected, long rootSeed, int pairs) throws Exception {
        final var economy = Step123ReferenceEconomy.configured();
        final var definitions = new ArrayList<Map<String, Object>>();
        for (final var value : selected) {
            final var definition = new LinkedHashMap<String, Object>();
            definition.put("condition", value);
            if (value.family().equals("FROZEN_CONTROL")) {
                definition.put("historicalCaseCode", value.code().substring("FROZEN_CONTROL_".length()));
            }
            definition.put("leftEquipment", exportTeam(value.left(), value.level(), value.disabledLeft(), economy));
            definition.put("rightEquipment", exportTeam(value.right(), value.level(), Set.of(), economy));
            definition.put("leftNominalGold", economy.nominalGold(items(value.left(), value.level())));
            definition.put("rightNominalGold", economy.nominalGold(items(value.right(), value.level())));
            definitions.add(definition);
        }
        final var manifest = new LinkedHashMap<String, Object>();
        manifest.put("schema", "Step123CorrectionsStudy/v1");
        manifest.put("rootSeed", rootSeed);
        manifest.put("pairsPerCondition", pairs);
        manifest.put("orientationsPerSeed", List.of(false, true));
        manifest.put("maxRounds", MAX_ROUNDS);
        manifest.put("combat", "SCALING_SKILLS_V2");
        manifest.put("engineScope", "Every condition uses the current corrected combat engine, including SCALING_V4 gear; "
            + "V4 conditions are equipment counterfactuals, not replays of the historical V4 engine");
        manifest.put("workingTiers", List.of(Tier.BEGINNER, Tier.MID, Tier.STRONG));
        manifest.put("technicalTiers", List.of(Tier.EXTENDED, Tier.STRESS));
        manifest.put("tierDefinitions", List.of(Tier.values()).stream().map(tier -> Map.of(
            "tier", tier, "group", tier.group(), "level", tier.level(), "profilePoints", tier.profilePoints())).toList());
        manifest.put("seedRule", "Step12RepresentativeSimulator.seed(rootSeed, condition.seedGroup, level, pair)");
        manifest.put("movementAudit", "Recover UUID positions from initialPositions and first-round tick order; "
            + "update every movement event; require abs(newLine-oldLine)<=1 for PersonageForcedMove; expect zero violations");
        manifest.put("pressureRearScope", "Diagnostic repetition of G,C,C versus G,D,D cycles at N3/7/15; not reference teams");
        manifest.put("nominalGoldScope", "Common bases plus rarity chains; excludes desired random modifiers and storm shards");
        manifest.put("economyProperties", economy.values());
        manifest.put("economyFingerprint", economy.fingerprint());
        manifest.put("cases", definitions);
        try (final var stream = Files.newOutputStream(output.resolve("cases.json"), StandardOpenOption.CREATE_NEW)) {
            JSON.writerWithDefaultPrettyPrinter().writeValue(stream, manifest);
        }
    }

    private static List<Item> items(List<Gear> team, int level) {
        return team.stream().flatMap(member -> member.items(level).stream()).toList();
    }

    private static List<Map<String, Object>> exportTeam(
        List<Gear> team,
        int level,
        Set<ActiveEnum> disabled,
        Step123ReferenceEconomy economy
    ) {
        return team.stream().map(member -> {
            final var configured = member.items(level);
            return Map.<String, Object>of(
                "build", member.build(), "catalog", member.catalog(), "configuredItems", configured,
                "effectiveItems", member.items(level, disabled), "nominalGold", economy.nominalGold(configured),
                "configuredSkillPoints", configured.stream().mapToInt(Item::skillPoints).sum()
            );
        }).toList();
    }

    private static CaseResult run(StudyCase value, long rootSeed, int pairs) {
        final var raw = new StringBuilder();
        final var scores = new ArrayList<Double>();
        final var leftTotals = new double[10];
        final var rightTotals = new double[10];
        final var outcomes = new int[4];
        long rounds = 0;
        int maxRounds = 0;
        long forcedMoves = 0;
        long violations = 0;
        long maxDistance = 0;
        long hitAndRun = 0;
        long retreat = 0;
        long knockback = 0;
        long leftReactions = 0;
        long rightReactions = 0;
        for (int pair = 0; pair < pairs; pair++) {
            final long seed = Step12RepresentativeSimulator.seed(rootSeed, value.seedGroup(), value.level(), pair);
            boolean resolved = true;
            double score = 0;
            for (final boolean reverse : List.of(false, true)) {
                final var observation = Step123BattleRunner.observe(value.left(), value.right(), value.level(), seed,
                    reverse, MAX_ROUNDS, value.disabledLeft(), Set.of());
                switch (observation.outcome()) {
                    case "WIN" -> outcomes[0]++;
                    case "LOSS" -> outcomes[1]++;
                    case "DRAW" -> outcomes[2]++;
                    case "TIMEOUT" -> {
                        outcomes[3]++;
                        resolved = false;
                    }
                    default -> throw new IllegalStateException("Unexpected battle outcome");
                }
                if (!observation.outcome().equals("TIMEOUT")) {
                    score += Step12RepresentativeSimulator.score(observation.outcome()) / 2;
                }
                final var audit = audit(observation.initialPositions(), observation.events(), reverse);
                final var reactions = reactionDamage(observation, reverse);
                forcedMoves += audit.forcedMoves();
                violations += audit.violations();
                maxDistance = Math.max(maxDistance, audit.maxDistance());
                hitAndRun += audit.hitAndRun();
                retreat += audit.retreat();
                knockback += audit.knockback();
                leftReactions += reactions[0];
                rightReactions += reactions[1];
                rounds += observation.rounds();
                maxRounds = Math.max(maxRounds, observation.rounds());
                add(leftTotals, values(observation.left()));
                add(rightTotals, values(observation.right()));
                raw.append(String.join("\t", value.code(), value.family(), value.tier(), "" + value.level(), "" + pair,
                    "" + seed, "" + reverse, observation.outcome(), "" + observation.rounds(), audit.fields(),
                    "" + reactions[0], "" + reactions[1], fields(values(observation.left()), 1),
                    fields(values(observation.right()), 1))).append('\n');
            }
            if (resolved) {
                scores.add(score);
            }
        }
        final var interval = Step12RepresentativeSimulator.estimate(scores);
        final int count = 2 * pairs;
        final var audit = new MovementAudit(forcedMoves, violations, maxDistance, hitAndRun, retreat, knockback);
        final var summary = String.join("\t", value.code(), value.family(), value.tier(), "" + value.level(),
            "" + value.left().size(), "" + pairs, "" + outcomes[0], "" + outcomes[1], "" + outcomes[2], "" + outcomes[3],
            "" + scores.size(), number(interval[0]), number(interval[1]), number(interval[2]), number(rounds / (double) count),
            "" + maxRounds, audit.fields(), number(leftReactions / (double) count), number(rightReactions / (double) count),
            fields(leftTotals, count), fields(rightTotals, count));
        return new CaseResult(summary, raw.toString(), outcomes[3], violations);
    }

    static MovementAudit audit(List<String> initialPositions, List<BattleEvent> events, boolean reverse) {
        final var positions = initialPositionsById(initialPositions, events, reverse);
        long count = 0;
        long violations = 0;
        long maximum = 0;
        long hitAndRun = 0;
        long retreat = 0;
        long knockback = 0;
        for (final var event : events) {
            if (event instanceof BattleEvent.MovedTowardEnemy move) {
                if (positions.replace(move.personageId(), move.newLineIndex()) == null) {
                    throw new IllegalArgumentException("Movement actor has no initial position: " + move.personageId());
                }
            } else if (event instanceof BattleEvent.PersonageForcedMove move) {
                final var before = positions.replace(move.personageId(), move.newLineIndex());
                if (before == null) {
                    throw new IllegalArgumentException("Forced movement actor has no initial position: " + move.personageId());
                }
                final long distance = Math.abs((long) move.newLineIndex() - before);
                count++;
                violations += distance > 1 ? 1 : 0;
                maximum = Math.max(maximum, distance);
                switch (move.skill()) {
                    case HIT_AND_RUN -> hitAndRun++;
                    case RETREAT -> retreat++;
                    case KNOCKBACK -> knockback++;
                    default -> { }
                }
            }
        }
        return new MovementAudit(count, violations, maximum, hitAndRun, retreat, knockback);
    }

    private static Map<UUID, Integer> initialPositionsById(
        List<String> initialPositions,
        List<BattleEvent> events,
        boolean reverse
    ) {
        final var firstTicks = events.stream().filter(event ->
            event instanceof BattleEvent.InitiativeAfterTick tick && tick.round() == 1)
            .map(BattleEvent.InitiativeAfterTick.class::cast).toList();
        if (firstTicks.size() != initialPositions.size()) {
            throw new IllegalArgumentException("Initial positions and first-round ticks must cover the same living roster");
        }
        final int leftSize = (int) initialPositions.stream().filter(value -> value.startsWith("LEFT:")).count();
        final int rightSize = initialPositions.size() - leftSize;
        final var result = new HashMap<UUID, Integer>();
        for (int index = 0; index < firstTicks.size(); index++) {
            final int logicalIndex = reverse ? index < rightSize ? leftSize + index : index - rightSize : index;
            final var definition = initialPositions.get(logicalIndex).split(":", 5);
            if (definition.length != 5 || !(definition[0].equals("LEFT") || definition[0].equals("RIGHT"))) {
                throw new IllegalArgumentException("Malformed initial position");
            }
            if (result.put(firstTicks.get(index).personageId(), Integer.parseInt(definition[4])) != null) {
                throw new IllegalArgumentException("Duplicate initial actor ID");
            }
        }
        return result;
    }

    private static long[] reactionDamage(Observation observation, boolean reverse) {
        final var initial = initialPositionsById(observation.initialPositions(), observation.events(), reverse);
        final int leftSize = (int) observation.initialPositions().stream().filter(value -> value.startsWith("LEFT:")).count();
        final var firstIds = observation.events().stream().filter(event ->
            event instanceof BattleEvent.InitiativeAfterTick tick && tick.round() == 1)
            .map(BattleEvent.InitiativeAfterTick.class::cast).map(BattleEvent.InitiativeAfterTick::personageId).toList();
        final int rightSize = initial.size() - leftSize;
        final var leftIds = Set.copyOf(firstIds.subList(reverse ? rightSize : 0, reverse ? firstIds.size() : leftSize));
        final var result = new long[2];
        for (final var event : observation.events()) {
            if (event instanceof BattleEvent.ScalingSkillDamage damage
                && (damage.skill() == ActiveEnum.COUNTER_ATTACK || damage.skill() == ActiveEnum.THORNS)) {
                result[leftIds.contains(damage.sourceId()) ? 0 : 1] += damage.damageTaken();
            } else if (event instanceof BattleEvent.SkillDamage damage
                && (damage.skill() == ActiveEnum.COUNTER_ATTACK || damage.skill() == ActiveEnum.THORNS)) {
                result[leftIds.contains(damage.sourceId()) ? 0 : 1] += damage.damageTaken();
            }
        }
        return result;
    }

    private static String sourceInputs() throws Exception {
        final var module = Files.isDirectory(Path.of("game/src")) ? Path.of("game") : Path.of(".");
        final List<Path> files;
        try (final var paths = Files.walk(module.resolve("src"))) {
            files = paths.filter(Files::isRegularFile).sorted(Comparator.comparing(Path::toString)).toList();
        }
        final var result = new StringBuilder();
        for (final var file : files) {
            result.append(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file))))
                .append("  ").append(module.relativize(file)).append('\n');
        }
        return result.toString();
    }

    private static double[] values(TeamMeasure value) {
        return new double[] {value.survivors(), value.remainingHealth(), value.actions(), value.attempts(), value.movementTurns(),
            value.damageTaken(), value.healing(), value.initiativeRemoved(), value.aliveTicks(), value.longestNoAttemptRounds()};
    }

    private static void add(double[] target, double[] values) {
        for (int index = 0; index < target.length; index++) {
            target[index] += values[index];
        }
    }

    private static String fields(double[] values, int count) {
        return String.join("\t", java.util.Arrays.stream(values).mapToObj(value -> number(value / count)).toList());
    }

    private static String number(double value) {
        return String.format(Locale.ROOT, "%.6f", value);
    }

    private static String metricHeader(String prefix) {
        return String.join("\t", List.of("survivors", "health", "actions", "attempts", "movementTurns", "damageTaken",
            "healing", "initiativeRemoved", "aliveTicks", "noAttemptGap").stream().map(value -> prefix + value).toList());
    }

    private static String auditHeader() {
        return "forcedMoves\tforcedMoveViolations\tmaxForcedDistance\thitAndRunMoves\tretreatMoves\tknockbackMoves";
    }

    record StudyCase(String code, String family, String tier, String seedGroup, int level, List<Gear> left,
                     List<Gear> right, Set<ActiveEnum> disabledLeft) {
        StudyCase {
            left = List.copyOf(left);
            right = List.copyOf(right);
            disabledLeft = Set.copyOf(disabledLeft);
            if (level < 0 || left.isEmpty() || left.size() != right.size()) {
                throw new IllegalArgumentException("Study teams must have equal positive size and a non-negative level");
            }
        }
    }

    record MovementAudit(long forcedMoves, long violations, long maxDistance, long hitAndRun, long retreat, long knockback) {
        String fields() {
            return String.join("\t", "" + forcedMoves, "" + violations, "" + maxDistance,
                "" + hitAndRun, "" + retreat, "" + knockback);
        }
    }

    private record CaseResult(String summary, String raw, int timeouts, long violations) {
    }
}
