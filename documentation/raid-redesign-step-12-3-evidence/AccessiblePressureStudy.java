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
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPOutputStream;
import ru.homyakin.seeker.game.battle.Step123BattleRunner.Gear;
import ru.homyakin.seeker.game.battle.Step12RepresentativeBattleRunner.TeamMeasure;
import ru.homyakin.seeker.game.battle.Step12RepresentativeBuilds.Assignment;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogVersion;
import ru.homyakin.seeker.game.item.models.ItemRarity;

/** Standalone economic comparison; run from game/ with one unused output prefix. No game source is changed. */
public final class AccessiblePressureStudy {
    private static final long ROOT = 2026102310L;
    private static final int PAIRS = 1000;
    private static final int MAX_ROUNDS = 10000;
    private static final String GROUP = "ACCESSIBLE_PRESSURE";
    private static final Path EVIDENCE = Path.of("../documentation/raid-redesign-step-12-3-evidence");
    private static final List<Integer> LEVELS = List.of(0, 10, 20);
    private static final List<String> METRICS = List.of("survivors", "health", "actions", "attempts", "movementTurns",
        "damageTaken", "healing", "initiativeRemoved", "aliveTicks", "noAttemptGap");

    private AccessiblePressureStudy() { }

    public static void main(String[] args) throws Exception {
        require(args.length == 1 && !args[0].isBlank(), "Specify one unused output prefix; working directory must be game/");
        final var prefix = Path.of(args[0]).toAbsolutePath().normalize();
        final var inputHashes = verifyInputs();
        for (final var suffix : List.of("-run.properties", "-cases.json.gz", "-battles.tsv.gz", "-summary.tsv", "-complete.properties")) {
            require(!Files.exists(Path.of(prefix + suffix)), "Output already exists: " + prefix + suffix);
        }
        final var source = Step123BattleStudy.cases().stream()
            .filter(value -> value.code().equals("MOBILE_ANSWER_B_FRONT_PRESSURE_L0")).findFirst().orElseThrow();
        final var left = source.left();
        final var right = new ArrayList<Gear>();
        right.add(new Gear(source.right().getFirst().build().withAssignments(Map.of()), EquipmentCatalogVersion.SCALING_V4));
        for (final var member : source.right().subList(1, 3)) {
            right.add(new Gear(member.build().withAssignments(Map.of("sledgehammer",
                new Assignment(ActiveEnum.BERSERK, ItemRarity.UNCOMMON))), EquipmentCatalogVersion.SCALING_V4));
        }
        require(left.getFirst().build().code().equals("MOBILE_spear_dirk_LEATHER"), "Unexpected evaluated build");
        require(right.getFirst().items(0).stream().allMatch(item -> item.rarity() == ItemRarity.COMMON
            && item.modifier().isEmpty()), "The guard must have only common unmodified gear");
        final var definitions = new ArrayList<Object>();
        for (final int level : LEVELS) {
            require(points(left, level) == 4 && points(right, level) == 4, "Both teams must have exactly four points");
            require(Step123BattleStudy.knownGold(left, level) == 3000, "Evaluated team cost changed");
            require(Step123BattleStudy.knownGold(right, level) == 2400, "Accessible response cost changed");
            require(left.stream().allMatch(member -> member.catalog() == EquipmentCatalogVersion.SCALING_V4), "Unexpected catalog");
            definitions.add(Map.of("level", level, "left", left, "right", right,
                "leftEquipment", left.stream().map(member -> member.items(level)).toList(),
                "rightEquipment", right.stream().map(member -> member.items(level)).toList(),
                "leftKnownGold", 3000, "rightKnownGold", 2400, "leftPoints", 4, "rightPoints", 4));
        }
        final var json = JsonMapper.builder().addModule(new Jdk8Module()).build();
        Files.writeString(Path.of(prefix + "-run.properties"), "seed=" + ROOT + "\npairs=" + PAIRS
            + "\ncases=3\nmaxRounds=" + MAX_ROUNDS + "\nseedGroup=" + GROUP + "\ncatalog=SCALING_V4\n"
            + "combat=SCALING_SKILLS_V2\nknownGoldExcludes=storm enhancement,rerolls,failures\n" + inputHashes,
            StandardOpenOption.CREATE_NEW);
        try (final var output = new GZIPOutputStream(Files.newOutputStream(Path.of(prefix + "-cases.json.gz"),
            StandardOpenOption.CREATE_NEW))) { json.writeValue(output, definitions); }
        final var summary = new StringBuilder("case\tlevel\tpairs\twins\tlosses\tdraws\ttimeouts\tresolvedPairs"
            + "\tscore\tlower\tupper\tunfinishedLow\tunfinishedHigh\tmeanRounds\tmaxRounds\t"
            + names("left_") + "\t" + names("right_") + "\n");
        int totalTimeouts = 0;
        try (final var raw = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(
            Path.of(prefix + "-battles.tsv.gz"), StandardOpenOption.CREATE_NEW)), StandardCharsets.UTF_8))) {
            raw.write("case\tlevel\tpair\tseed\treverse\toutcome\trounds\t" + names("left_") + "\t" + names("right_") + "\n");
            for (final int level : LEVELS) {
                int wins = 0, losses = 0, draws = 0, timeouts = 0, longest = 0;
                long rounds = 0;
                double knownScore = 0;
                final var scores = new ArrayList<Double>();
                final var leftTotals = new long[METRICS.size()];
                final var rightTotals = new long[METRICS.size()];
                for (int pair = 0; pair < PAIRS; pair++) {
                    final long seed = Step12RepresentativeSimulator.seed(ROOT, GROUP, level, pair);
                    double pairScore = 0;
                    boolean resolved = true;
                    for (final boolean reverse : List.of(false, true)) {
                        final var observed = Step123BattleRunner.observe(left, right, level, seed, reverse, MAX_ROUNDS, Set.of(), Set.of());
                        switch (observed.outcome()) {
                            case "WIN" -> wins++;
                            case "LOSS" -> losses++;
                            case "DRAW" -> draws++;
                            case "TIMEOUT" -> { timeouts++; resolved = false; }
                            default -> throw new IllegalStateException("Unexpected outcome: " + observed.outcome());
                        }
                        if (!observed.outcome().equals("TIMEOUT")) {
                            final double score = Step12RepresentativeSimulator.score(observed.outcome());
                            pairScore += score / 2; knownScore += score;
                        }
                        rounds += observed.rounds(); longest = Math.max(longest, observed.rounds());
                        add(leftTotals, values(observed.left())); add(rightTotals, values(observed.right()));
                        raw.write(GROUP + "\t" + level + "\t" + pair + "\t" + seed + "\t" + reverse + "\t"
                            + observed.outcome() + "\t" + observed.rounds() + "\t" + fields(values(observed.left()), 1)
                            + "\t" + fields(values(observed.right()), 1) + "\n");
                    }
                    if (resolved) { scores.add(pairScore); }
                }
                final var estimate = Step12RepresentativeSimulator.estimate(scores);
                final int battles = PAIRS * 2;
                summary.append(GROUP).append('\t').append(level).append('\t').append(PAIRS).append('\t')
                    .append(wins).append('\t').append(losses).append('\t').append(draws).append('\t').append(timeouts)
                    .append('\t').append(scores.size()).append('\t').append(estimate[0]).append('\t').append(estimate[1])
                    .append('\t').append(estimate[2]).append('\t').append(knownScore / battles).append('\t')
                    .append((knownScore + timeouts) / battles).append('\t').append(rounds / (double) battles).append('\t')
                    .append(longest).append('\t').append(fields(leftTotals, battles)).append('\t')
                    .append(fields(rightTotals, battles)).append('\n');
                totalTimeouts += timeouts;
                System.out.println("Completed level " + level + "; wins=" + wins + "; losses=" + losses + "; timeouts=" + timeouts);
            }
        }
        Files.writeString(Path.of(prefix + "-summary.tsv"), summary, StandardOpenOption.CREATE_NEW);
        require(inputHashes.equals(verifyInputs()), "Source inputs changed during the study; no completion receipt");
        Files.writeString(Path.of(prefix + "-complete.properties"), "cases=3\nbattles=6000\ntimeouts=" + totalTimeouts
            + "\nseed=" + ROOT + "\n" + inputHashes, StandardOpenOption.CREATE_NEW);
    }

    private static String verifyInputs() throws Exception {
        final var manifest = EVIDENCE.resolve("final-source-inputs.sha256");
        for (final var line : Files.readAllLines(manifest)) {
            final int separator = line.indexOf("  ");
            require(separator == 64, "Invalid source manifest row: " + line);
            final var path = Path.of(line.substring(separator + 2));
            require(line.substring(0, separator).equals(sha256(path)), "Frozen source differs: " + path);
        }
        return "sourceSha256=" + sha256(EVIDENCE.resolve("AccessiblePressureStudy.java"))
            + "\nfinalManifestSha256=" + sha256(manifest) + "\n";
    }

    private static String sha256(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }

    private static int points(List<Gear> team, int level) {
        return team.stream().flatMap(member -> member.items(level).stream()).mapToInt(item -> item.skillPoints()).sum();
    }

    private static long[] values(TeamMeasure team) {
        return new long[] {team.survivors(), team.remainingHealth(), team.actions(), team.attempts(), team.movementTurns(),
            team.damageTaken(), team.healing(), team.initiativeRemoved(), team.aliveTicks(), team.longestNoAttemptRounds()};
    }

    private static void add(long[] total, long[] values) { for (int index = 0; index < values.length; index++) { total[index] += values[index]; } }
    private static String names(String prefix) { return String.join("\t", METRICS.stream().map(value -> prefix + value).toList()); }
    private static String fields(long[] values, int divisor) {
        return String.join("\t", java.util.Arrays.stream(values).mapToObj(value -> Double.toString(value / (double) divisor)).toList());
    }
    private static void require(boolean condition, String message) { if (!condition) { throw new IllegalStateException(message); } }
}
