package ru.homyakin.seeker.game.battle;

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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;
import ru.homyakin.seeker.game.battle.Step123BattleRunner.Gear;
import ru.homyakin.seeker.game.battle.Step12RepresentativeBattleRunner.TeamMeasure;
import ru.homyakin.seeker.game.battle.Step12RepresentativeBuilds.Assignment;
import ru.homyakin.seeker.game.battle.Step12RepresentativeBuilds.Build;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.RoleBuild;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.V3Build;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.targeting.TargetingTactic;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogVersion;
import ru.homyakin.seeker.game.item.models.ItemRarity;

/** Local catalog correction and useful mobile builds; no universal win-rate acceptance gate. */
public final class Step123BattleStudy {
    private static final JsonMapper JSON = JsonMapper.builder()
        .addModule(new Jdk8Module()).addModule(new JavaTimeModule()).build();
    private static final List<Integer> LEVELS = List.of(0, 10, 20);

    private Step123BattleStudy() {
    }

    static List<StudyCase> cases() {
        final var result = new ArrayList<StudyCase>();
        addCloth(result);
        addMobile(result);
        addMobileNiche(result);
        addMobileAnswers(result);
        addControls(result);
        result.sort(Comparator.comparing(StudyCase::code));
        if (result.stream().map(StudyCase::code).distinct().count() != result.size()) {
            throw new IllegalStateException("Duplicate study case code");
        }
        return List.copyOf(result);
    }

    static List<Build> mobileBuilds() {
        final var result = new ArrayList<Build>();
        result.add(Step12RepresentativeBuilds.role(RoleBuild.SKIRMISHER));
        final var choices = List.of(
            List.of("sword", "dirk", "LEATHER"), List.of("sword", "dirk", "NEW_LEATHER"),
            List.of("sword", "shield", "LEATHER"), List.of("sword", "shortsword", "LEATHER"),
            List.of("sword", "shortsword", "CLOTH"), List.of("rapier", "dirk", "LEATHER"),
            List.of("mace", "dirk", "LEATHER"), List.of("staff", "orb", "ARCANE"),
            List.of("spear", "dirk", "LEATHER")
        );
        for (final var choice : choices) {
            final var armor = Step12RepresentativeBuilds.armor(choice.get(2));
            result.add(Step12RepresentativeBuilds.hand(
                "MOBILE_" + String.join("_", choice), choice.subList(0, 2), choice.get(2),
                Position.FRONT, TargetingTactic.THREAT
            ).withAssignments(Map.of(
                armor.get(2), new Assignment(ActiveEnum.HIT_AND_RUN, ItemRarity.RARE),
                armor.get(3), new Assignment(ActiveEnum.HIT_AND_RUN, ItemRarity.RARE)
            )));
        }
        return List.copyOf(result);
    }

    static List<Build> mobileNicheBuilds() {
        final var result = new ArrayList<Build>();
        result.add(historicalV3Build(V3Build.SKIRMISHER_SLASH));
        final var selected = Set.of(
            "MOBILE_sword_shortsword_CLOTH", "MOBILE_sword_shortsword_LEATHER",
            "MOBILE_sword_dirk_LEATHER", "MOBILE_sword_dirk_NEW_LEATHER", "MOBILE_spear_dirk_LEATHER"
        );
        mobileBuilds().stream().filter(build -> selected.contains(build.code())).forEach(result::add);
        return List.copyOf(result);
    }

    static Build historicalV3Build(V3Build build) {
        final var items = Step12SimulationFixtures.v3Items(build, 0);
        final var assignments = new LinkedHashMap<String, Assignment>();
        items.forEach(item -> item.modifier().ifPresent(modifier -> assignments.put(
            item.object().code(), new Assignment(modifier.activeEnum(), item.rarity())
        )));
        return Step12RepresentativeBuilds.custom(
            "HISTORICAL_V3_" + build.name(), items.stream().map(item -> item.object().code()).toList(),
            build.position(), build.targetingTactic(), assignments
        );
    }

    private static void addMobileNiche(List<StudyCase> result) {
        final var support = historicalV3Build(V3Build.CROSSBOW_SUPPORT).withPosition(Position.MID);
        for (final var build : mobileNicheBuilds()) {
            for (final var opponent : List.of(
                V3Build.BREAKER_CLOSE_SLASH, V3Build.BRUISER_BLUNT, V3Build.ASSASSIN_PIERCE
            )) {
                final var group = "MOBILE_NICHE_" + build.code() + "_CROSSBOW_MID_" + opponent;
                final var left = gear(List.of(build.withPosition(Position.FRONT), support, support),
                    EquipmentCatalogVersion.SCALING_V4);
                final var right = gear(List.of(historicalV3Build(opponent).withPosition(Position.FRONT), support, support),
                    EquipmentCatalogVersion.SCALING_V4);
                for (final int level : LEVELS) {
                    for (final boolean disabled : List.of(false, true)) {
                        result.add(new StudyCase(group + "_L" + level + (disabled ? "_OFF" : ""),
                            "MOBILE_NICHE", group, level, left, right,
                            disabled ? Set.of(ActiveEnum.HIT_AND_RUN) : Set.of()));
                    }
                }
            }
        }
    }

    private static void addMobileAnswers(List<StudyCase> result) {
        final var mobile = mobileNicheBuilds().stream()
            .filter(build -> build.code().equals("MOBILE_spear_dirk_LEATHER")).findFirst().orElseThrow();
        final var support = historicalV3Build(V3Build.CROSSBOW_SUPPORT).withPosition(Position.MID);
        final var left = gear(List.of(mobile.withPosition(Position.FRONT), support, support),
            EquipmentCatalogVersion.SCALING_V4);
        final var guardian = Step12RepresentativeBuilds.role(RoleBuild.GUARDIAN);
        final var bruiser = Step12RepresentativeBuilds.role(RoleBuild.BRUISER);
        final var responses = Map.of(
            "A_ANCHORED_BRUISER", Step12RepresentativeMatrix.anchored(bruiser),
            "B_FRONT_PRESSURE", List.of(guardian, bruiser, bruiser),
            "C_PROTECTED_RANGERS", List.of(
                guardian, Step12RepresentativeBuilds.role(RoleBuild.RANGER_PHYSICAL).withPosition(Position.MID),
                Step12RepresentativeBuilds.role(RoleBuild.RANGER_MAGICAL).withPosition(Position.MID)
            )
        );
        for (final var entry : responses.entrySet()) {
            final var group = "MOBILE_ANSWER_" + entry.getKey();
            final var right = gear(entry.getValue(), EquipmentCatalogVersion.SCALING_V4);
            for (final int level : LEVELS) {
                for (final boolean disabled : List.of(false, true)) {
                    result.add(new StudyCase(group + "_L" + level + (disabled ? "_OFF" : ""),
                        "MOBILE_ANSWER", group, level, left, right,
                        disabled ? Set.of(ActiveEnum.HIT_AND_RUN) : Set.of()));
                }
            }
        }
    }

    private static void addCloth(List<StudyCase> result) {
        for (final var skill : List.of(ActiveEnum.TEMPO_BREAK, ActiveEnum.KNOCKBACK)) {
            final String weapon = skill == ActiveEnum.TEMPO_BREAK ? "command_staff" : "bow";
            for (final var opponent : List.of(
                Step12RepresentativeMatrix.opponents().get(1), Step12RepresentativeMatrix.opponents().get(3)
            )) {
                final var group = "CLOTH_" + skill + "_" + opponent.code();
                for (final var variant : List.of("OLD", "NEW_V3", "NEW_V4")) {
                    final var catalog = variant.equals("NEW_V3")
                        ? EquipmentCatalogVersion.SCALING_V3 : EquipmentCatalogVersion.SCALING_V4;
                    final var build = Step12RepresentativeBuilds.hand(variant, List.of(weapon),
                        variant.equals("OLD") ? "CLOTH" : "NEW_CLOTH", Position.BACK, TargetingTactic.THREAT)
                        .withAssignments(Map.of(weapon, new Assignment(skill, ItemRarity.RARE)));
                    for (final int level : LEVELS) {
                        result.add(new StudyCase(group + "_" + variant + "_L" + level, "CLOTH", group, level,
                            gear(Step12RepresentativeMatrix.anchored(build), catalog),
                            gear(Step12RepresentativeMatrix.anchored(opponent), EquipmentCatalogVersion.SCALING_V4), Set.of()));
                    }
                }
            }
        }
        for (final var pair : Step12RepresentativeBuilds.armorPairs().stream()
            .filter(pair -> pair.oldCode().equals("robe") || pair.oldCode().equals("cloth_chausses")).toList()) {
            final var oldBuild = Step12RepresentativeBuilds.hand("ONE_OLD", List.of("rapier", "dagger"),
                "CLOTH", Position.FRONT, TargetingTactic.THREAT);
            final var newBuild = oldBuild.replaceItem(pair.oldCode(), pair.newCode()).renamed("ONE_NEW");
            for (final var opponent : Step12RepresentativeMatrix.opponents()) {
                for (final int level : LEVELS) {
                    final var group = "SINGLE_" + pair.oldCode() + "_" + opponent.code();
                    for (final var variant : List.of("OLD", "NEW_V4")) {
                        result.add(new StudyCase(group + "_" + variant + "_L" + level, "SINGLE_CLOTH", group, level,
                            gear(List.of(variant.equals("OLD") ? oldBuild : newBuild), EquipmentCatalogVersion.SCALING_V4),
                            gear(List.of(opponent), EquipmentCatalogVersion.SCALING_V4), Set.of()));
                    }
                }
            }
        }
    }

    private static void addMobile(List<StudyCase> result) {
        for (final var build : mobileBuilds()) {
            for (final var opponent : List.of(
                RoleBuild.BRUISER, RoleBuild.BREAKER_PHYSICAL, RoleBuild.ASSASSIN, RoleBuild.RANGER_MAGICAL
            )) {
                for (final var environment : List.of("PASSIVE", "PROTECTED", "PRESSURE", "SOLE_FRONT")) {
                    final var group = build.code() + "_" + environment + "_" + opponent;
                    final var left = mobileTeam(build, environment);
                    final var right = mobileTeam(Step12RepresentativeBuilds.role(opponent), environment);
                    for (final int level : LEVELS) {
                        for (final boolean disabled : List.of(false, true)) {
                            result.add(new StudyCase(group + "_L" + level + (disabled ? "_OFF" : ""), "MOBILE", group, level,
                                gear(left, EquipmentCatalogVersion.SCALING_V4), gear(right, EquipmentCatalogVersion.SCALING_V4),
                                disabled ? Set.of(ActiveEnum.HIT_AND_RUN) : Set.of()));
                        }
                    }
                }
            }
        }
    }

    private static List<Build> mobileTeam(Build build, String environment) {
        return switch (environment) {
            case "PASSIVE" -> Step12RepresentativeMatrix.anchored(build);
            case "PROTECTED" -> List.of(
                Step12RepresentativeBuilds.role(RoleBuild.GUARDIAN),
                build.position() == Position.BACK ? build.withPosition(Position.MID) : build,
                Step12RepresentativeBuilds.role(RoleBuild.RANGER_PHYSICAL).withPosition(Position.MID)
            );
            case "PRESSURE" -> List.of(
                Step12RepresentativeBuilds.role(RoleBuild.GUARDIAN), build.withPosition(Position.FRONT),
                Step12RepresentativeBuilds.role(RoleBuild.BRUISER)
            );
            case "SOLE_FRONT" -> List.of(
                build.withPosition(Position.FRONT),
                Step12RepresentativeBuilds.role(RoleBuild.RANGER_PHYSICAL).withPosition(Position.MID),
                Step12RepresentativeBuilds.role(RoleBuild.RANGER_MAGICAL).withPosition(Position.MID)
            );
            default -> throw new IllegalArgumentException("Unknown team environment");
        };
    }

    private static void addControls(List<StudyCase> result) {
        for (final var case12 : Step12RepresentativeFollowup.scenarios()) {
            if (case12.family().equals("ACTIVE_SUPPORT") || (case12.family().equals("HAND_RESPONSE")
                && (case12.code().contains("sword+shield") || case12.code().contains("COMMAND_NEW_LEATHER")))) {
                result.add(new StudyCase("CONTROL_" + case12.code(), "CONTROL", "CONTROL_" + case12.seedGroup(), case12.level(),
                    gear(case12.left(), EquipmentCatalogVersion.SCALING_V4),
                    gear(case12.right(), EquipmentCatalogVersion.SCALING_V4), Set.of()));
            }
        }
    }

    private static List<Gear> gear(List<Build> builds, EquipmentCatalogVersion version) {
        return builds.stream().map(build -> new Gear(build, version)).toList();
    }

    public static void main(String[] args) throws Exception {
        final long root = Long.getLong("step123.seed", 2026102304L);
        final int pairs = Integer.getInteger("step123.pairs", 100);
        final int workers = Integer.getInteger("step123.workers", 4);
        final var filter = Pattern.compile(System.getProperty("step123.filter", ".*"));
        final var prefix = Path.of(System.getProperty("step123.output",
            "../documentation/raid-redesign-step-12-3-evidence/diagnostic")).toAbsolutePath().normalize();
        final var selected = cases().stream().filter(value -> filter.matcher(value.code()).find()).toList();
        if (selected.isEmpty() || pairs < 2 || workers < 1 || Files.exists(Path.of(prefix + "-run.properties"))) {
            throw new IllegalArgumentException("Invalid study configuration or output prefix already used");
        }
        Files.createDirectories(prefix.getParent());
        final var inputs = inputs();
        Files.writeString(Path.of(prefix + "-inputs.sha256"), inputs, StandardOpenOption.CREATE_NEW);
        Files.writeString(Path.of(prefix + "-run.properties"), "seed=" + root + "\npairs=" + pairs
            + "\nfilter=" + filter + "\ncases=" + selected.size() + "\nmaxRounds=10000\n", StandardOpenOption.CREATE_NEW);
        final var definitions = new ArrayList<Object>();
        for (final var value : selected) {
            definitions.add(Map.of("case", value,
                "leftEquipment", value.left().stream().map(gear -> gear.items(value.level())).toList(),
                "rightEquipment", value.right().stream().map(gear -> gear.items(value.level())).toList(),
                "leftKnownGold", knownGold(value.left(), value.level()),
                "rightKnownGold", knownGold(value.right(), value.level())));
        }
        try (final var output = new GZIPOutputStream(Files.newOutputStream(Path.of(prefix + "-cases.json.gz"),
            StandardOpenOption.CREATE_NEW))) {
            JSON.writeValue(output, definitions);
        }
        final var summaries = new ArrayList<String>();
        final long started = System.nanoTime();
        long battles = 0;
        long timeouts = 0;
        try (final var writer = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(
            Path.of(prefix + "-battles.tsv.gz"), StandardOpenOption.CREATE_NEW)), StandardCharsets.UTF_8));
             final var executor = Executors.newFixedThreadPool(workers)) {
            writer.write("case\tpair\tseed\treverse\toutcome\trounds\thitRunMoves\t" + metrics("left_")
                + "\t" + metrics("right_") + "\n");
            final var completions = new ExecutorCompletionService<CaseResult>(executor);
            for (final var value : selected) {
                completions.submit(() -> run(value, root, pairs));
            }
            for (int done = 1; done <= selected.size(); done++) {
                final var result = completions.take().get();
                summaries.add(result.summary());
                writer.write(result.raw());
                battles += pairs * 2L;
                timeouts += result.timeouts();
                if (done % 50 == 0 || done == selected.size()) {
                    System.out.printf(java.util.Locale.ROOT, "Completed %d/%d; battles=%d; timeouts=%d; seconds=%.1f%n",
                        done, selected.size(), battles, timeouts, (System.nanoTime() - started) / 1e9);
                }
            }
        }
        summaries.sort(Comparator.naturalOrder());
        Files.writeString(Path.of(prefix + "-summary.tsv"),
            "case\tfamily\tlevel\tpairs\twins\tlosses\tdraws\ttimeouts\tresolvedPairs\tscore\tlower\tupper\tmeanRounds"
                + "\tmaxRounds\thitRunMoves\t" + metrics("left_") + "\t" + metrics("right_") + "\n"
                + String.join("\n", summaries) + "\n", StandardOpenOption.CREATE_NEW);
        if (!inputs.equals(inputs())) {
            throw new IllegalStateException("Source inputs changed during this study; no completion receipt written");
        }
        Files.writeString(Path.of(prefix + "-complete.properties"), "cases=" + selected.size() + "\nbattles=" + battles
            + "\ntimeouts=" + timeouts + "\n", StandardOpenOption.CREATE_NEW);
    }

    private static CaseResult run(StudyCase value, long root, int pairs) {
        final var raw = new StringBuilder();
        final var scores = new ArrayList<Double>();
        final var leftTotals = new double[10];
        final var rightTotals = new double[10];
        int wins = 0;
        int losses = 0;
        int draws = 0;
        int timeouts = 0;
        long rounds = 0;
        int maxRounds = 0;
        long hitRunMoves = 0;
        for (int pair = 0; pair < pairs; pair++) {
            final long seed = Step12RepresentativeSimulator.seed(root, value.seedGroup(), value.level(), pair);
            boolean resolved = true;
            double score = 0;
            for (final boolean reverse : List.of(false, true)) {
                final var observation = Step123BattleRunner.observe(value.left(), value.right(), value.level(), seed,
                    reverse, 10_000, value.disabledLeft(), Set.of());
                switch (observation.outcome()) {
                    case "WIN" -> wins++;
                    case "LOSS" -> losses++;
                    case "DRAW" -> draws++;
                    case "TIMEOUT" -> {
                        timeouts++;
                        resolved = false;
                    }
                    default -> throw new IllegalStateException("Unexpected outcome");
                }
                if (!observation.outcome().equals("TIMEOUT")) {
                    score += Step12RepresentativeSimulator.score(observation.outcome()) / 2;
                }
                final long moves = observation.events().stream().filter(event ->
                    event instanceof BattleEvent.PersonageForcedMove movement && movement.skill() == ActiveEnum.HIT_AND_RUN).count();
                hitRunMoves += moves;
                rounds += observation.rounds();
                maxRounds = Math.max(maxRounds, observation.rounds());
                add(leftTotals, values(observation.left()));
                add(rightTotals, values(observation.right()));
                raw.append(value.code()).append('\t').append(pair).append('\t').append(seed).append('\t').append(reverse)
                    .append('\t').append(observation.outcome()).append('\t').append(observation.rounds()).append('\t').append(moves)
                    .append('\t').append(fields(values(observation.left()), 1))
                    .append('\t').append(fields(values(observation.right()), 1)).append('\n');
            }
            if (resolved) {
                scores.add(score);
            }
        }
        final var interval = Step12RepresentativeSimulator.estimate(scores);
        final int count = 2 * pairs;
        return new CaseResult(String.join("\t", value.code(), value.family(), "" + value.level(), "" + pairs,
            "" + wins, "" + losses, "" + draws, "" + timeouts, "" + scores.size(), number(interval[0]),
            number(interval[1]), number(interval[2]), number(rounds / (double) count), "" + maxRounds,
            number(hitRunMoves / (double) count), fields(leftTotals, count), fields(rightTotals, count)), raw.toString(), timeouts);
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
        return String.format(java.util.Locale.ROOT, "%.6f", value);
    }

    private static String metrics(String prefix) {
        return String.join("\t", List.of("survivors", "health", "actions", "attempts", "movementTurns", "damageTaken",
            "healing", "initiativeRemoved", "aliveTicks", "noAttemptGap").stream().map(field -> prefix + field).toList());
    }

    private static String inputs() throws Exception {
        final var paths = new ArrayList<Path>();
        try (final var stream = Files.walk(Path.of("src/main"))) {
            paths.addAll(stream.filter(Files::isRegularFile).toList());
        }
        try (final var stream = Files.list(Path.of("src/test/java/ru/homyakin/seeker/game/battle"))) {
            paths.addAll(stream.filter(path -> path.getFileName().toString().startsWith("Step123")
                || path.getFileName().toString().startsWith("Step12Representative")).toList());
        }
        paths.add(Path.of("src/test/java/ru/homyakin/seeker/game/battle/simulation/Step12ScalingV3Fixtures.java"));
        paths.add(Path.of("src/test/java/ru/homyakin/seeker/game/battle/simulation/Step12SimulationFixtures.java"));
        paths.sort(Comparator.comparing(Path::toString));
        final var result = new StringBuilder();
        for (final var path : paths) {
            result.append(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path))))
                .append("  ").append(path).append('\n');
        }
        return result.toString();
    }

    static long knownGold(List<Gear> team, int level) throws Exception {
        final var properties = new Properties();
        try (final var input = Files.newInputStream(Path.of("src/main/resources/application.properties"))) {
            properties.load(input);
        }
        long total = 0;
        for (final var member : team) {
            for (final var item : member.items(level)) {
                total += Long.parseLong(properties.getProperty("homyakin.seeker.shop.commonPrice"))
                    * 2 * item.object().slots().size();
                var rarity = ItemRarity.COMMON;
                while (rarity != item.rarity()) {
                    rarity = rarity.next().orElseThrow();
                    final var key = rarity.name().toLowerCase(java.util.Locale.ROOT) + "Price";
                    total += (int) (Integer.parseInt(properties.getProperty("homyakin.seeker.shop." + key)) * 1.5);
                }
            }
        }
        return total;
    }

    record StudyCase(String code, String family, String seedGroup, int level, List<Gear> left, List<Gear> right,
                     Set<ActiveEnum> disabledLeft) {
        StudyCase {
            left = List.copyOf(left);
            right = List.copyOf(right);
            disabledLeft = Set.copyOf(disabledLeft);
            if (left.isEmpty() || left.size() != right.size()) {
                throw new IllegalArgumentException("Both study teams must have the same non-zero size");
            }
        }
    }

    private record CaseResult(String summary, String raw, int timeouts) {
    }
}
