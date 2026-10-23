package ru.homyakin.seeker.game.battle;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;
import ru.homyakin.seeker.game.battle.Step12RepresentativeBattleRunner.Observation;
import ru.homyakin.seeker.game.battle.Step12RepresentativeBattleRunner.TeamMeasure;
import ru.homyakin.seeker.game.battle.Step12RepresentativeBuilds.Build;
import ru.homyakin.seeker.game.battle.Step12RepresentativeMatrix.Scenario;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.item.storm.StormEnhanceConfig;
import ru.homyakin.seeker.game.shop.ShopConfig;

/** Repeatable diagnostic study. No 50/50 acceptance gate and no mutation of historical evidence. */
public final class Step12RepresentativeSimulator {
    private static final JsonMapper JSON = JsonMapper.builder()
        .addModule(new Jdk8Module()).addModule(new JavaTimeModule()).build();
    private static final String MEASURE_HEADER =
        "survivors\thealth\tactions\tattempts\tmovementTurns\tdamageTaken\thealing\tinitiativeRemoved\taliveTicks\tnoAttemptGap";

    private Step12RepresentativeSimulator() {
    }

    public static void main(String[] args) throws Exception {
        final long rootSeed = Long.getLong("step12.rep.seed", 2026102301L);
        final int maxRounds = Integer.getInteger("step12.rep.maxRounds", 2000);
        final int workers = Integer.getInteger("step12.rep.workers", 4);
        final int override = Integer.getInteger("step12.rep.iterations", 0);
        final var filter = Pattern.compile(System.getProperty("step12.rep.filter", ".*"));
        final var prefix = Path.of(System.getProperty("step12.rep.output",
            "../documentation/raid-redesign-step-12-2-primary")).toAbsolutePath().normalize();
        final var matrix = Boolean.getBoolean("step12.rep.followup")
            ? Step12RepresentativeFollowup.scenarios() : Step12RepresentativeMatrix.scenarios();
        final var cases = matrix.stream()
            .filter(scenario -> filter.matcher(scenario.code()).find()).toList();
        if (cases.isEmpty() || workers < 1 || maxRounds < 1 || override < 0 || override == 1) {
            throw new IllegalArgumentException("Invalid study configuration or empty selection");
        }
        Files.createDirectories(prefix.getParent());
        final var definitions = new ArrayList<Map<String, Object>>();
        for (final var scenario : cases) {
            definitions.add(Map.of(
                "scenario", scenario,
                "leftEquipment", equipment(scenario.left(), scenario.level()),
                "rightEquipment", equipment(scenario.right(), scenario.level()),
                "pairs", override > 0 ? override : scenario.iterations()
            ));
        }
        try (final var output = new GZIPOutputStream(Files.newOutputStream(Path.of(prefix + "-cases.json.gz")))) {
            JSON.writeValue(output, definitions);
        }
        final var inputs = inputManifest();
        Files.writeString(Path.of(prefix + "-inputs.sha256"), inputs);
        Files.writeString(Path.of(prefix + "-run.properties"),
            "rootSeed=" + rootSeed + "\nmaxRounds=" + maxRounds + "\nworkers=" + workers
                + "\niterationsOverride=" + override + "\nfollowup=" + Boolean.getBoolean("step12.rep.followup")
                + "\nfilter=" + filter + "\nscenarios=" + cases.size()
                + "\ninputsSha256=" + sha256(inputs.getBytes(StandardCharsets.UTF_8))
                + "\ninterval=95% per-case Hoeffding bound over independent paired scores; not simultaneous"
                + "\ntimeout=separate unfinished result; never a loss or excluded without a count\n");
        final var rows = new ArrayList<String>();
        final long started = System.nanoTime();
        long battles = 0;
        long timeouts = 0;
        try (final var raw = gzip(Path.of(prefix + "-battles.tsv.gz"));
             final var executor = Executors.newFixedThreadPool(workers)) {
            raw.write("case\tpair\tseed\treverse\toutcome\trounds\t" + prefixed("left_")
                + "\t" + prefixed("right_") + "\n");
            final var completions = new ExecutorCompletionService<CaseResult>(executor);
            for (final var scenario : cases) {
                completions.submit(() -> run(scenario, rootSeed, maxRounds, override, prefix));
            }
            for (int completed = 1; completed <= cases.size(); completed++) {
                final var result = completions.take().get();
                rows.add(result.summary());
                raw.write(result.raw());
                battles += result.battles();
                timeouts += result.timeouts();
                if (completed % 50 == 0 || completed == cases.size()) {
                    System.out.printf(Locale.ROOT, "Completed %d/%d cases; battles=%d timeouts=%d elapsed=%.1fs%n",
                        completed, cases.size(), battles, timeouts, (System.nanoTime() - started) / 1_000_000_000.0);
                }
            }
        }
        rows.sort(Comparator.naturalOrder());
        Files.writeString(Path.of(prefix + "-summary.tsv"), summaryHeader() + "\n" + String.join("\n", rows) + "\n");
        if (!inputs.equals(inputManifest())) {
            throw new IllegalStateException("Study inputs changed during the run; do not accept these results");
        }
        Files.writeString(Path.of(prefix + "-complete.properties"),
            "scenarios=" + cases.size() + "\nbattles=" + battles + "\ntimeouts=" + timeouts
                + "\nelapsedSeconds=" + (System.nanoTime() - started) / 1_000_000_000.0 + "\n");
    }

    private static CaseResult run(Scenario scenario, long rootSeed, int maxRounds, int override, Path prefix) throws Exception {
        final int pairs = override > 0 ? override : scenario.iterations();
        final var raw = new StringBuilder();
        final var pairedScores = new ArrayList<Double>();
        final var rounds = new ArrayList<Integer>();
        final var left = new Totals();
        final var right = new Totals();
        int wins = 0;
        int losses = 0;
        int draws = 0;
        int timeouts = 0;
        int firstWins = 0;
        boolean exampleSaved = false;
        for (int pair = 0; pair < pairs; pair++) {
            final long seed = seed(rootSeed, scenario.seedGroup(), scenario.level(), pair);
            double pairedScore = 0;
            boolean resolved = true;
            for (final boolean reverse : List.of(false, true)) {
                final var observation = Step12RepresentativeBattleRunner.observe(
                    scenario.left(), scenario.right(), scenario.level(), seed, reverse, maxRounds,
                    scenario.disabledLeft(), scenario.disabledRight()
                );
                switch (observation.outcome()) {
                    case "WIN" -> wins++;
                    case "LOSS" -> losses++;
                    case "DRAW" -> draws++;
                    case "TIMEOUT" -> {
                        timeouts++;
                        resolved = false;
                    }
                    default -> throw new IllegalStateException("Unknown outcome");
                }
                if ((!reverse && observation.outcome().equals("WIN")) || (reverse && observation.outcome().equals("LOSS"))) {
                    firstWins++;
                }
                if (!observation.outcome().equals("TIMEOUT")) {
                    pairedScore += score(observation.outcome()) / 2;
                }
                rounds.add(observation.rounds());
                left.add(observation.left());
                right.add(observation.right());
                raw.append(scenario.code()).append('\t').append(pair).append('\t').append(seed).append('\t')
                    .append(reverse).append('\t').append(observation.outcome()).append('\t').append(observation.rounds())
                    .append('\t').append(measure(observation.left())).append('\t').append(measure(observation.right())).append('\n');
                if (!exampleSaved && (observation.outcome().equals("TIMEOUT")
                    || (scenario.family().equals("RISK") && observation.rounds() >= 100))) {
                    try (final var output = new GZIPOutputStream(Files.newOutputStream(
                        Path.of(prefix + "-example-" + scenario.code() + ".json.gz")))) {
                        JSON.writeValue(output, new Example(scenario, pair, seed, reverse, observation));
                    }
                    exampleSaved = true;
                }
            }
            if (resolved) {
                pairedScores.add(pairedScore);
            }
        }
        rounds.sort(Comparator.naturalOrder());
        final var estimate = estimate(pairedScores);
        final int count = pairs * 2;
        final double knownScore = wins + draws * 0.5;
        final String summary = String.join("\t", scenario.code(), scenario.family(), "" + scenario.level(), "" + pairs,
            "" + wins, "" + losses, "" + draws, "" + timeouts, "" + pairedScores.size(),
            number(estimate[0]), number(estimate[1]), number(estimate[2]),
            number(knownScore / count), number((knownScore + timeouts) / count),
            number(rounds.stream().mapToInt(Integer::intValue).average().orElseThrow()),
            "" + rounds.get((int) Math.ceil(0.95 * count) - 1), "" + rounds.getLast(),
            number(firstWins / (double) count), left.means(count), right.means(count));
        return new CaseResult(summary, raw.toString(), count, timeouts);
    }

    static double score(String outcome) {
        return switch (outcome) {
            case "WIN" -> 1;
            case "LOSS" -> 0;
            case "DRAW" -> 0.5;
            default -> throw new IllegalArgumentException("Unfinished battle has no score: " + outcome);
        };
    }

    static double[] estimate(List<Double> scores) {
        if (scores.isEmpty()) {
            return new double[] {Double.NaN, 0, 1};
        }
        final double mean = scores.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
        final double radius = Math.sqrt(Math.log(40) / (2 * scores.size()));
        return new double[] {mean, Math.max(0, mean - radius), Math.min(1, mean + radius)};
    }

    static long seed(long root, String group, int level, int index) {
        long hash = 0xcbf29ce484222325L;
        for (final byte value : group.getBytes(StandardCharsets.UTF_8)) {
            hash = (hash ^ (value & 255)) * 0x100000001b3L;
        }
        final long domain = mix64(mix64(root) ^ hash ^ ((long) level << 32));
        return mix64(domain + 0x9e3779b97f4a7c15L * index);
    }

    private static long mix64(long input) {
        long value = input;
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }

    private static List<Map<String, Object>> equipment(List<Build> team, int level) throws IOException {
        final var shop = shop();
        final var enhance = new StormEnhanceConfig();
        final var result = new ArrayList<Map<String, Object>>();
        for (final var build : team) {
            final var items = build.items(level);
            final int baseGold = items.stream().mapToInt(item -> shop.specificObjectBuyPrice(item.object().slots().size()).value()).sum();
            int rarityGold = 0;
            for (final var item : items) {
                var rarity = ItemRarity.COMMON;
                while (rarity != item.rarity()) {
                    rarity = rarity.next().orElseThrow();
                    rarityGold += (int) (shop.buyingPriceByRarity(rarity).value() * 1.5);
                }
            }
            final int nextAttempt = items.stream().mapToInt(item -> enhance.costForLevel(level, item.object().slots()).value()).sum();
            result.add(Map.of("build", build, "items", items, "commonPurchaseGold", baseGold,
                "rarityUpgradeGold", rarityGold, "nextEnhanceAttemptShards", nextAttempt,
                "skillPoints", items.stream().mapToInt(item -> item.skillPoints()).sum()));
        }
        return result;
    }

    private static ShopConfig shop() throws IOException {
        final var properties = new Properties();
        try (final var input = Files.newInputStream(Path.of("src/main/resources/application.properties"))) {
            properties.load(input);
        }
        final var result = new ShopConfig();
        result.setCommonPrice(Integer.parseInt(properties.getProperty("homyakin.seeker.shop.commonPrice")));
        result.setUncommonPrice(Integer.parseInt(properties.getProperty("homyakin.seeker.shop.uncommonPrice")));
        result.setRarePrice(Integer.parseInt(properties.getProperty("homyakin.seeker.shop.rarePrice")));
        result.setEpicPrice(Integer.parseInt(properties.getProperty("homyakin.seeker.shop.epicPrice")));
        result.setLegendaryPrice(Integer.parseInt(properties.getProperty("homyakin.seeker.shop.legendaryPrice")));
        return result;
    }

    private static String inputManifest() throws Exception {
        final var paths = new ArrayList<Path>();
        try (final var files = Files.walk(Path.of("src/main"))) {
            paths.addAll(files.filter(Files::isRegularFile).toList());
        }
        try (final var files = Files.list(Path.of("src/test/java/ru/homyakin/seeker/game/battle"))) {
            paths.addAll(files.filter(path -> path.getFileName().toString().startsWith("Step12Representative")).toList());
        }
        paths.add(Path.of("src/test/java/ru/homyakin/seeker/game/battle/simulation/Step12ScalingV3Fixtures.java"));
        paths.add(Path.of("src/test/java/ru/homyakin/seeker/game/battle/simulation/Step12SimulationFixtures.java"));
        paths.add(Path.of("../documentation/raid-redesign-balance-policy.md"));
        paths.sort(Comparator.comparing(Path::toString));
        final var result = new StringBuilder();
        for (final var path : paths) {
            result.append(sha256(Files.readAllBytes(path))).append("  ").append(path).append('\n');
        }
        return result.toString();
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static BufferedWriter gzip(Path path) throws IOException {
        return new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(path)), StandardCharsets.UTF_8));
    }

    private static String measure(TeamMeasure measure) {
        return String.join("\t", "" + measure.survivors(), "" + measure.remainingHealth(), "" + measure.actions(),
            "" + measure.attempts(), "" + measure.movementTurns(), "" + measure.damageTaken(), "" + measure.healing(),
            "" + measure.initiativeRemoved(), "" + measure.aliveTicks(), "" + measure.longestNoAttemptRounds());
    }

    private static String prefixed(String prefix) {
        return String.join("\t", List.of(MEASURE_HEADER.split("\t")).stream().map(field -> prefix + field).toList());
    }

    private static String summaryHeader() {
        return "case\tfamily\tlevel\tpairs\twins\tlosses\tdraws\ttimeouts\tresolvedPairs\tpairedScore\tscoreLow\tscoreHigh"
            + "\tunfinishedLow\tunfinishedHigh\tmeanRounds\tp95Rounds\tmaxRounds\tfirstSideWinRate\t"
            + prefixed("left_") + "\t" + prefixed("right_");
    }

    private static String number(double value) {
        return String.format(Locale.ROOT, "%.6f", value);
    }

    private record CaseResult(String summary, String raw, int battles, int timeouts) {
    }

    private record Example(Scenario scenario, int pair, long seed, boolean reverse, Observation observation) {
    }

    private static final class Totals {
        private final double[] values = new double[10];

        void add(TeamMeasure measure) {
            final var fields = measure(measure).split("\t");
            for (int index = 0; index < fields.length; index++) {
                values[index] += Double.parseDouble(fields[index]);
            }
        }

        String means(int count) {
            final var fields = new ArrayList<String>();
            for (final double value : values) {
                fields.add(number(value / count));
            }
            return String.join("\t", fields);
        }
    }
}
