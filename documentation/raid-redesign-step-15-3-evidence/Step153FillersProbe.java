package ru.homyakin.seeker.game.battle;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
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
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffects;
import ru.homyakin.seeker.utils.RandomUtils;

/** Independent append-only weak-member study using unchanged production generators and Battle. */
public final class Step153FillersProbe {
    private static final List<String> ALL_STYLES = List.of("MAIN", "PIERCE_ARCANE", "RAPIER_ARCANE", "RITUAL", "WEAK_ONLY");
    private static final List<Integer> ALL_LEVELS = List.of(1, 5, 10, 15, 20, 25, 30, 40, 50);
    private static final List<Integer> ALL_SIZES = List.of(1, 2, 3, 7, 15, 30);
    private static final long GENERATION_ROOT = 6_026_110_815_300L;
    private static final long BATTLE_ROOT = 7_026_110_815_300L;

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Specify a new output directory");
        final var output = Path.of(args[0]);
        Files.createDirectory(output);
        final int pairs = Integer.getInteger("fillers.pairs", 20);
        if (pairs < 1 || pairs > 1000) throw new IllegalArgumentException("Pairs must be 1..1000");
        final long seedOffset = Long.getLong("fillers.seedOffset", 0L);
        final var styles = strings("fillers.styles", ALL_STYLES);
        final var levels = integers("fillers.levels", ALL_LEVELS);
        final var sizes = integers("fillers.sizes", ALL_SIZES);
        if (!sizes.contains(1)) throw new IllegalArgumentException("The unchanged solo control size 1 is required");
        final var weakMethod = MyconidAdaptationStudy.class.getDeclaredMethod("weakWithDefaults");
        weakMethod.setAccessible(true);
        final var weak = (InputMember) weakMethod.invoke(null);
        validateWeak(weak);
        final var allInputs = new TreeMap<String, Object>();
        for (final var style : styles) {
            final var strong = style.equals("WEAK_ONLY") ? weak : PveAdaptationStudy.inputFor(
                new Cell(Mode.RAID, "MYCONID_COLONY", style, Tier.STRONG, 1, 1, false)).getFirst();
            final var strongPersonage = prepared(strong);
            final var inputsBySize = new TreeMap<String, Object>();
            for (final int size : sizes) inputsBySize.put(Integer.toString(size), members(strong, weak, size));
            allInputs.put(style, Map.of("fixedFirstMember", strong, "firstSkills", strongPersonage.skillSnapshots(),
                "firstHealth", strongPersonage.maxHealth(), "weakHealth", prepared(weak).maxHealth(),
                "inputsBySize", inputsBySize));
        }
        Files.writeString(output.resolve("inputs.json"), json(allInputs), StandardOpenOption.CREATE_NEW);
        final var manifest = new LinkedHashMap<String, Object>();
        manifest.put("purpose", "APPEND_ONLY_LIVE_WEAK_MEMBERS_NOT_EQUAL_COST_OR_UNIVERSAL_BALANCE");
        manifest.put("generationRoot", GENERATION_ROOT);
        manifest.put("battleRoot", BATTLE_ROOT);
        manifest.put("seedOffset", seedOffset);
        manifest.put("seedPolicy", "ROOT + offset + fixedStyleIndex*1000000 + level*10000 + pair; size deliberately absent");
        manifest.put("pairPolicy", "Fresh battle objects per mirror; two orders are one independent pair; common seeds across sizes");
        manifest.put("firstMemberPolicy", "Always inputFor(style,STRONG,size=1), never regenerated as a larger main/counter team");
        manifest.put("weakPolicy", "Original weakWithDefaults helper; common +0 sword plus six defaults; exactly 840 HP and seven slots");
        manifest.put("catalog", "SCALING_V5");
        manifest.put("skills", "SCALING_SKILLS_V3");
        manifest.put("tempo", "ACCELERATED_V1");
        manifest.put("styles", styles);
        manifest.put("levels", levels);
        manifest.put("sizes", sizes);
        manifest.put("pairs", pairs);
        manifest.put("confidencePolicy", "Pointwise 95% Hoeffding on pairs; difference range [-1,1]; no simultaneous guarantee");
        Files.writeString(output.resolve("manifest.json"), json(manifest), StandardOpenOption.CREATE_NEW);
        final var summaries = new ArrayList<Map<String, Object>>();
        final var differences = new ArrayList<Map<String, Object>>();
        try (final var raw = writer(output.resolve("battles.jsonl.gz"))) {
            for (final var style : styles) {
                final var strong = style.equals("WEAK_ONLY") ? weak : PveAdaptationStudy.inputFor(
                    new Cell(Mode.RAID, "MYCONID_COLONY", style, Tier.STRONG, 1, 1, false)).getFirst();
                for (final int level : levels) {
                    List<Double> baseScores = null;
                    for (final int size : sizes) {
                        final var input = members(strong, weak, size);
                        final var result = run(style, level, size, pairs, seedOffset, input, raw);
                        summaries.add(result);
                        @SuppressWarnings("unchecked") final var scores = (List<Double>) result.get("pairScores");
                        if (size == 1) baseScores = scores;
                        else differences.add(difference(style, level, size, baseScores, scores));
                        System.out.println("completed " + summaries.size() + " " + style + " L" + level + " N" + size
                            + " WIN=" + result.get("wins") + "/" + (pairs * 2));
                    }
                }
            }
        }
        Files.writeString(output.resolve("summary.json"), json(Map.of("manifest", manifest, "cases", summaries,
            "pairedAppendDifferences", differences)), StandardOpenOption.CREATE_NEW);
        System.out.println("COMPLETE cases=" + summaries.size() + " battles=" + summaries.size() * pairs * 2);
    }

    private static Map<String, Object> run(String style, int level, int size, int pairs, long seedOffset,
        List<InputMember> members, BufferedWriter raw) throws Exception {
        final var counts = new TreeMap<String, Integer>();
        final var scores = new ArrayList<Double>();
        final var rounds = new ArrayList<Integer>();
        final var cell = new Cell(Mode.RAID, "MYCONID_COLONY", "FILLERS_" + style, Tier.STRONG, size, level, false);
        for (int pair = 0; pair < pairs; ++pair) {
            final long key = seedOffset + ALL_STYLES.indexOf(style) * 1_000_000L + level * 10_000L + pair;
            double pairScore = 0;
            boolean complete = true;
            for (final boolean reverse : List.of(false, true)) {
                final var row = PveAdaptationStudy.runWithMembers(cell, members,
                    GENERATION_ROOT + key, BATTLE_ROOT + key, reverse);
                row.put("style", style);
                row.put("pair", pair);
                row.put("seedOffset", seedOffset);
                final var basis = (List<?>) row.get("playerBasis");
                for (int index = style.equals("WEAK_ONLY") ? 0 : 1; index < basis.size(); ++index) {
                    if (((Number) ((Map<?, ?>) basis.get(index)).get("health")).intValue() != 840)
                        throw new IllegalStateException("Weak participant did not start with 840 HP");
                }
                final String outcome = (String) row.get("outcome");
                counts.merge(outcome, 1, Integer::sum);
                if (outcome.equals("TIMEOUT")) complete = false;
                else {
                    pairScore += outcome.equals("WIN") ? 0.5 : outcome.equals("DRAW") ? 0.25 : 0;
                    rounds.add((Integer) row.get("rounds"));
                }
                raw.write(PveAdaptationStudy.JSON.writeValueAsString(row));
                raw.newLine();
            }
            scores.add(complete ? pairScore : null);
        }
        final var completeScores = scores.stream().filter(java.util.Objects::nonNull).toList();
        final double mean = completeScores.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        final double half = completeScores.isEmpty() ? 1 : Math.sqrt(Math.log(40) / (2 * completeScores.size()));
        final var result = new LinkedHashMap<String, Object>();
        result.put("style", style); result.put("level", level); result.put("size", size);
        result.put("pairs", pairs); result.put("completePairs", completeScores.size());
        result.put("wins", counts.getOrDefault("WIN", 0)); result.put("losses", counts.getOrDefault("LOSS", 0));
        result.put("draws", counts.getOrDefault("DRAW", 0)); result.put("timeouts", counts.getOrDefault("TIMEOUT", 0));
        result.put("winrate", counts.getOrDefault("WIN", 0) / (2.0 * pairs));
        result.put("pairScoreMean", mean); result.put("pairScores", scores);
        result.put("pointwisePairScore95Hoeffding", List.of(Math.max(0, mean - half), Math.min(1, mean + half)));
        result.put("meanRounds", rounds.stream().mapToInt(Integer::intValue).average().orElse(0));
        return result;
    }

    private static Map<String, Object> difference(String style, int level, int size, List<Double> base, List<Double> added) {
        final var differences = new ArrayList<Double>();
        for (int index = 0; index < base.size(); ++index)
            if (base.get(index) != null && added.get(index) != null) differences.add(added.get(index) - base.get(index));
        final double mean = differences.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        final double half = differences.isEmpty() ? 2 : Math.sqrt(2 * Math.log(40) / differences.size());
        return Map.of("style", style, "level", level, "size", size, "completePairs", differences.size(),
            "meanDifference", mean, "pointwisePairedDifference95Hoeffding",
            List.of(Math.max(-1, mean - half), Math.min(1, mean + half)), "pairDifferences", differences);
    }

    private static List<InputMember> members(InputMember first, InputMember weak, int size) {
        final var result = new ArrayList<InputMember>();
        result.add(first);
        while (result.size() < size) result.add(weak);
        if (!result.getFirst().equals(first)) throw new IllegalStateException("The first strong input changed");
        return List.copyOf(result);
    }

    private static void validateWeak(InputMember weak) {
        final var slots = EnumSet.noneOf(PersonageSlot.class);
        for (final var item : weak.items()) {
            if (item.rarity() != ItemRarity.COMMON || item.enhanceLevel() != 0 || item.modifier().isPresent())
                throw new IllegalStateException("Weak participant must have common +0 items without modifiers");
            for (final var slot : item.object().slots()) if (!slots.add(slot)) throw new IllegalStateException("Duplicate slot");
        }
        if (weak.items().size() != 7 || !slots.equals(EnumSet.allOf(PersonageSlot.class))
            || prepared(weak).maxHealth() != 840 || !prepared(weak).isAlive()) throw new IllegalStateException("Invalid live weak input");
    }

    private static BattlePersonage prepared(InputMember member) {
        return RandomUtils.withSeed(0, () -> BattlePersonage.forCombat(member.items(), member.position(),
            PersonageEffects.EMPTY, Optional.of(member.build()), PveAdaptationStudy.FORMULA));
    }

    private static List<String> strings(String name, List<String> defaults) {
        final var value = System.getProperty(name);
        final var result = value == null ? defaults : Arrays.stream(value.split(",")).map(String::trim).toList();
        if (result.isEmpty() || result.stream().distinct().count() != result.size() || !defaults.containsAll(result))
            throw new IllegalArgumentException("Unsupported " + name);
        return result;
    }

    private static List<Integer> integers(String name, List<Integer> defaults) {
        final var value = System.getProperty(name);
        final var result = value == null ? defaults : Arrays.stream(value.split(",")).map(String::trim).map(Integer::parseInt).toList();
        if (result.isEmpty() || result.stream().distinct().count() != result.size() || !defaults.containsAll(result))
            throw new IllegalArgumentException("Unsupported " + name);
        return result;
    }

    private static BufferedWriter writer(Path path) throws Exception {
        return new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(path,
            StandardOpenOption.CREATE_NEW)), StandardCharsets.UTF_8));
    }

    private static String json(Object value) throws Exception {
        return PveAdaptationStudy.JSON.writerWithDefaultPrettyPrinter().writeValueAsString(value);
    }
}
