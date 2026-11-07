package ru.homyakin.seeker.game.battle;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;
import ru.homyakin.seeker.game.battle.Step123BattleRunner.Gear;
import ru.homyakin.seeker.game.battle.Step12RepresentativeBuilds.Assignment;
import ru.homyakin.seeker.game.battle.Step12RepresentativeBuilds.Build;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.targeting.TargetingTactic;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogVersion;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.utils.RandomUtils;

/** Temporary diagnostic. No production/test class is replaced or shadowed by this helper. */
public final class Nov07ComparativeStudy {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final int MAX_ROUNDS = Integer.getInteger("audit.maxRounds", 3000);
    private static final BattleTempoVersion TEMPO = BattleTempoVersion.ACCELERATED_V1;
    private static final EquipmentCatalogVersion CATALOG = EquipmentCatalogVersion.valueOf(System.getProperty("audit.catalog", "SCALING_V5"));
    private static final Set<ActiveEnum> NONE = Set.of();

    private record Case(String code, String family, String budget, int level, List<Gear> left,
                        List<Gear> right, String seedGroup, Set<ActiveEnum> disabledLeft) { }
    private record Mover(BattlePersonage personage, Map<UUID, BattlePersonage> enemy) { }
    private record Result(String outcome, int rounds,
                          Step12RepresentativeBattleRunner.TeamMeasure left,
                          Step12RepresentativeBattleRunner.TeamMeasure right,
                          long firstDeath, long forcedMoves, long violations, long maxDistance,
                          long leftMaxNonAttackOwnTurns, long rightMaxNonAttackOwnTurns,
                          long leftNeverAttacked, long rightNeverAttacked,
                          Map<String, Long> skillDamage, Map<String, Long> skillEvents) { }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Specify a new output prefix");
        final var prefix = Path.of(args[0]).toAbsolutePath();
        final int pairs = Integer.getInteger("audit.pairs", 12);
        final long root = Long.getLong("audit.seed", 2026110701L);
        final var filter = Pattern.compile(System.getProperty("audit.filter", ".*"));
        final var economy = Step123ReferenceEconomy.configured();
        final var selected = cases().stream().filter(c -> filter.matcher(c.code()).find()).toList();
        if (selected.isEmpty() || pairs < 1) throw new IllegalArgumentException("No cases or invalid pairs");
        final var summaries = new ArrayList<Map<String, Object>>();
        final var definitions = new ArrayList<Map<String, Object>>();
        final var provenance = provenance();
        JSON.writerWithDefaultPrettyPrinter().writeValue(Files.newOutputStream(Path.of(prefix + "-provenance.json"),
            StandardOpenOption.CREATE_NEW), provenance);
        try (final var raw = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(
            Path.of(prefix + "-pairs.jsonl.gz"), StandardOpenOption.CREATE_NEW)), StandardCharsets.UTF_8))) {
            for (final var c : selected) {
                final long leftCost = cost(c.left(), c.level(), economy);
                final long rightCost = cost(c.right(), c.level(), economy);
                if (c.family().equals("MATRIX") && leftCost != rightCost)
                    throw new IllegalStateException("Unequal exact nominal costs: " + c.code());
                definitions.add(map("case", c, "leftNominalGold", leftCost, "rightNominalGold", rightCost,
                    "leftEquipment", manifest(c.left(), c.level(), economy),
                    "rightEquipment", manifest(c.right(), c.level(), economy)));
                final var values = new ArrayList<Result>();
                final var pairedScores = new ArrayList<Double>();
                for (int pair = 0; pair < pairs; pair++) {
                    final long seed = mix(root + c.seedGroup().hashCode(), pair);
                    double pairScore = 0;
                    for (final boolean reverse : List.of(false, true)) {
                        final var r = observe(c, seed, reverse);
                        values.add(r);
                        pairScore += score(r.outcome()) / 2;
                        raw.write(JSON.writeValueAsString(map("case", c.code(), "pair", pair,
                            "reverse", reverse, "seed", seed, "result", r)));
                        raw.newLine();
                    }
                    pairedScores.add(pairScore);
                }
                raw.flush();
                final var outcomes = new LinkedHashMap<String, Long>();
                for (final var r : values) outcomes.merge(r.outcome(), 1L, Long::sum);
                final var summary = map("case", c.code(), "family", c.family(), "budget", c.budget(),
                    "leftBuild", c.left().get(c.left().size()-1).build().code(), "rightBuild", c.right().get(c.right().size()-1).build().code(),
                    "size", c.left().size(), "level", c.level(), "leftNominalGold", leftCost,
                    "rightNominalGold", rightCost, "pairs", pairs, "outcomes", outcomes,
                    "score", pairedScores.stream().mapToDouble(Double::doubleValue).average().orElseThrow(),
                    "pairedHoeffding95", pairInterval(pairedScores),
                    "rounds", stats(values.stream().mapToLong(Result::rounds).toArray()),
                    "actions", stats(values.stream().mapToLong(r -> r.left().actions() + r.right().actions()).toArray()),
                    "firstDeath", stats(values.stream().mapToLong(Result::firstDeath).toArray()),
                    "leftMaxNonAttackOwnTurns", stats(values.stream().mapToLong(Result::leftMaxNonAttackOwnTurns).toArray()),
                    "rightMaxNonAttackOwnTurns", stats(values.stream().mapToLong(Result::rightMaxNonAttackOwnTurns).toArray()),
                    "leftNeverAttacked", stats(values.stream().mapToLong(Result::leftNeverAttacked).toArray()),
                    "rightNeverAttacked", stats(values.stream().mapToLong(Result::rightNeverAttacked).toArray()),
                    "leftHealing", stats(values.stream().mapToLong(r -> r.left().healing()).toArray()),
                    "rightHealing", stats(values.stream().mapToLong(r -> r.right().healing()).toArray()),
                    "leftMovement", stats(values.stream().mapToLong(r -> r.left().movementTurns()).toArray()),
                    "rightMovement", stats(values.stream().mapToLong(r -> r.right().movementTurns()).toArray()),
                    "leftInitiativeRemoved", stats(values.stream().mapToLong(r -> r.left().initiativeRemoved()).toArray()),
                    "rightInitiativeRemoved", stats(values.stream().mapToLong(r -> r.right().initiativeRemoved()).toArray()),
                    "forcedMoveViolations", values.stream().mapToLong(Result::violations).sum(),
                    "forcedMoves", values.stream().mapToLong(Result::forcedMoves).sum());
                summaries.add(summary);
                System.err.println(summaries.size() + "/" + selected.size() + " " + c.code() + " " + outcomes);
            }
        }
        try (final var out = new GZIPOutputStream(Files.newOutputStream(Path.of(prefix + "-cases.json.gz"),
            StandardOpenOption.CREATE_NEW))) { JSON.writeValue(out, definitions); }
        JSON.writerWithDefaultPrettyPrinter().writeValue(Files.newOutputStream(Path.of(prefix + "-summary.json"),
            StandardOpenOption.CREATE_NEW), map("catalog", CATALOG, "combat", "SCALING_SKILLS_V3",
            "tempo", TEMPO, "multiplier", TEMPO.damageMultiplier(), "tempoAssertions", "every combatant in every battle",
            "rootSeed", root, "pairs", pairs, "filter", filter.pattern(), "cases", selected.size(),
            "battles", selected.size() * pairs * 2, "maxRounds", MAX_ROUNDS,
            "seedSchedule", "SplitMix64(root + JavaHash(seedGroup),pairIndex); identical left/right swap and causal variants",
            "nominalGoldScope", "Common bases plus rarity-upgrade chain; excludes desired-modifier search, shards, enhancement acquisition",
            "confidence", "95% per-cell Hoeffding bound on independent mirrored-seed pair scores in [0,1], delta=sqrt(log(40)/(2*pairs)); not simultaneous across selected cases",
            "summaries", summaries));
    }

    private static Result observe(Case c, long seed, boolean reverse) {
        return RandomUtils.withSeed(seed, () -> {
            final var left = c.left().stream().map(g -> g.personage(c.level(), c.disabledLeft())).toList();
            final var right = c.right().stream().map(g -> g.personage(c.level(), NONE)).toList();
            final var first = reverse ? right : left;
            final var second = reverse ? left : right;
            final var random = new SeededBattleRandom(seed);
            final var context = new BattleContext(first, second, random, TEMPO);
            if (context.tempoVersion() != TEMPO || TEMPO.damageMultiplier() != 1.75)
                throw new IllegalStateException("Context is not accelerated");
            for (final var team : List.of(left, right)) for (final var p : team)
                if (p.battleTempoVersion() != TEMPO || p.battleTempoVersion().damageMultiplier() != 1.75)
                    throw new IllegalStateException("Personage is not accelerated");
            final var log = new BattleActionLog();
            final var positions = new ArrayList<String>();
            positions(positions, "LEFT", c.left(), left);
            positions(positions, "RIGHT", c.right(), right);
            int rounds = 0;
            while (!context.firstAliveTeam().isEmpty() && !context.secondAliveTeam().isEmpty() && rounds < MAX_ROUNDS) {
                ++rounds;
                log.add(new BattleEvent.RoundStarted(rounds));
                final var movers = new ArrayList<Mover>();
                for (final var p : context.firstAliveTeam().values()) if (p.tick(log, rounds))
                    movers.add(new Mover(p, context.secondAliveTeam()));
                for (final var p : context.secondAliveTeam().values()) if (p.tick(log, rounds))
                    movers.add(new Mover(p, context.firstAliveTeam()));
                for (final var mover : random.shuffle("turn-order:" + rounds, movers)) {
                    if (mover.enemy().isEmpty()) break;
                    if (mover.personage().move(context, log, rounds)) break;
                    context.pruneDefeated();
                }
            }
            final var lm = Step12RepresentativeBattleRunner.measure(left, log.events(), log.traceEvents());
            final var rm = Step12RepresentativeBattleRunner.measure(right, log.events(), log.traceEvents());
            final String outcome = lm.survivors() > 0 && rm.survivors() > 0 ? "TIMEOUT"
                : lm.survivors() > 0 ? "WIN" : rm.survivors() > 0 ? "LOSS" : "DRAW";
            final var audit = Step123CorrectionsStudy.audit(positions, log.events(), reverse);
            final long[] la = actorStats(left, log.traceEvents());
            final long[] ra = actorStats(right, log.traceEvents());
            final var damage = new LinkedHashMap<String, Long>();
            final var effects = new LinkedHashMap<String, Long>();
            for (final var e : log.events()) {
                if (e instanceof BattleEvent.ScalingSkillDamage s) damage.merge(s.skill().name(), (long) s.damageTaken(), Long::sum);
                if (e instanceof BattleEvent.EffectDamage s) damage.merge(s.skill().name(), (long) s.damageTaken(), Long::sum);
                if (e instanceof BattleEvent.SkillDamage s) damage.merge(s.skill().name(), (long) s.damageTaken(), Long::sum);
                if (e instanceof BattleEvent.SkillWindowUsed s) effects.merge(s.skill().name(), 1L, Long::sum);
                if (e instanceof BattleEvent.PersonageForcedMove s) effects.merge(s.skill().name(), 1L, Long::sum);
            }
            return new Result(outcome, rounds, lm, rm,
                log.events().stream().filter(e -> e instanceof BattleEvent.PersonageDefeated).mapToLong(BattleEvent::round).min().orElse(-1),
                audit.forcedMoves(), audit.violations(), audit.maxDistance(), la[0], ra[0], la[1], ra[1], damage, effects);
        });
    }

    private static void positions(List<String> out, String side, List<Gear> gear, List<BattlePersonage> team) {
        for (int i = 0; i < team.size(); i++) out.add("%s:%d:%s:%s:%d".formatted(
            side, i, gear.get(i).build().code(), team.get(i).startPosition(), team.get(i).currentPosition()));
    }

    private static long[] actorStats(List<BattlePersonage> team, List<BattleTraceEvent> trace) {
        final var attempted = new HashSet<Long>();
        for (final var e : trace) if (e instanceof BattleTraceEvent.NormalAttackAttempt a) attempted.add(a.turnId());
        long max = 0, never = 0;
        for (final var p : team) {
            long current = 0, attacks = 0;
            for (final var e : trace) if (e instanceof BattleTraceEvent.TurnFinished t
                && t.personageId().equals(p.id()) && t.actionPerformed()) {
                if (attempted.contains(t.turnId())) { attacks++; current = 0; }
                else { current++; max = Math.max(max, current); }
            }
            if (attacks == 0) never++;
        }
        return new long[]{max, never};
    }

    private static List<Case> cases() {
        final var result = new ArrayList<Case>();
        final var focus = Pattern.compile(System.getProperty("audit.focus", ".*"));
        final var profiles = Pattern.compile(System.getProperty("audit.profiles", ".*"));
        for (final String budget : List.of("RARE", "HEAVY")) {
            final var builds = pool(budget.equals("HEAVY"));
            for (final int level : ints("audit.levels", "0,6")) for (final int size : ints("audit.sizes", "7")) {
                for (int i = 0; i < builds.size(); i++) for (int j = i + 1; j < builds.size(); j++) {
                    if (!profiles.matcher(builds.get(i).code()).matches() || !profiles.matcher(builds.get(j).code()).matches()) continue;
                    if (!focus.matcher(builds.get(i).code()).find() && !focus.matcher(builds.get(j).code()).find()) continue;
                    final String code = "MATRIX_" + budget + "_N" + size + "_L" + level + "_" + builds.get(i).code() + "_VS_" + builds.get(j).code();
                    result.add(new Case(code, "MATRIX", budget, level, repeat(builds.get(i), size),
                        repeat(builds.get(j), size), budget + "_N" + size + "_L" + level, NONE));
                }
                for (final String skill : List.of("HIT_AND_RUN", "BLEEDING", "SELF_HEAL", "KNOCKBACK", "TEMPO_BREAK")) {
                    final var offense = builds.stream().filter(b -> b.assignments().values().stream().anyMatch(a -> a.skill().name().equals(skill))).findFirst().orElseThrow();
                    final var defense = builds.stream().filter(b -> b.code().equals("BRUISER_HEAL")).findFirst().orElseThrow();
                    for (final boolean off : List.of(false, true)) {
                        final String group = "CAUSAL_" + budget + "_N" + size + "_L" + level + "_" + skill;
                        result.add(new Case(group + (off ? "_OFF" : "_ON"), "CAUSAL", budget, level,
                            repeat(offense, size), repeat(defense, size), group,
                            off ? Set.of(ActiveEnum.valueOf(skill)) : NONE));
                    }
                }
            }
        }
        return result.stream().sorted(Comparator.comparing(Case::code)).toList();
    }

    private static List<Build> pool(boolean heavy) {
        final var rarity = heavy ? ItemRarity.LEGENDARY : ItemRarity.RARE;
        final var out = new ArrayList<Build>();
        for (final var skill : List.of(ActiveEnum.PENETRATION, ActiveEnum.DOUBLE_ATTACK, ActiveEnum.BLEEDING, ActiveEnum.ACCUMULATION)) {
            final var a = new LinkedHashMap<String, Assignment>();
            a.put("rapier", new Assignment(skill, rarity)); a.put("hood", new Assignment(ActiveEnum.HIT_AND_RUN, rarity));
            if (heavy) { a.put("dirk", new Assignment(skill, rarity)); a.put("cloth_boots", new Assignment(ActiveEnum.HIT_AND_RUN, rarity)); }
            out.add(Step12RepresentativeBuilds.hand("MOBILE_" + skill, List.of("rapier", "dirk"), "CLOTH", Position.FRONT,
                TargetingTactic.EXECUTIONER).withAssignments(a));
        }
        for (final String family : List.of("ARCANE", "PLATE")) for (final var skill : List.of(ActiveEnum.PENETRATION, ActiveEnum.BLEEDING)) {
            final var armor = Step12RepresentativeBuilds.armor(family);
            final var a = new LinkedHashMap<String, Assignment>();
            a.put("rapier", new Assignment(skill, rarity)); a.put(armor.get(3), new Assignment(ActiveEnum.HIT_AND_RUN, rarity));
            if (heavy) { a.put("dirk", new Assignment(skill, rarity)); a.put(armor.get(2), new Assignment(ActiveEnum.HIT_AND_RUN, rarity)); }
            out.add(Step12RepresentativeBuilds.hand("MOBILE_" + family + "_" + skill, List.of("rapier", "dirk"), family,
                Position.FRONT, TargetingTactic.EXECUTIONER).withAssignments(a));
        }
        record Spec(String code, List<String> hands, String armor, Position position, ActiveEnum skill, TargetingTactic tactic) { }
        for (final var spec : List.of(
            new Spec("BRUISER_HEAL", List.of("sledgehammer"), "LEATHER", Position.FRONT, ActiveEnum.BERSERK, TargetingTactic.WOUNDED_HUNTER),
            new Spec("COMMAND_HEAL", List.of("command_staff"), "LEATHER", Position.FRONT, ActiveEnum.BERSERK, TargetingTactic.WOUNDED_HUNTER),
            new Spec("COMMAND_FEINT_HEAL", List.of("command_staff"), "LEATHER", Position.FRONT, ActiveEnum.BERSERK, TargetingTactic.WOUNDED_HUNTER),
            new Spec("COMMAND_ARCANE", List.of("command_staff"), "ARCANE", Position.FRONT, ActiveEnum.BERSERK, TargetingTactic.WOUNDED_HUNTER),
            new Spec("COMMAND_NEW_ARCANE", List.of("command_staff"), "NEW_ARCANE", Position.FRONT, ActiveEnum.BERSERK, TargetingTactic.WOUNDED_HUNTER),
            new Spec("COMMAND_PLATE", List.of("command_staff"), "PLATE", Position.FRONT, ActiveEnum.BERSERK, TargetingTactic.WOUNDED_HUNTER),
            new Spec("COMMAND_PLATE_BLEED", List.of("command_staff"), "PLATE", Position.FRONT, ActiveEnum.BLEEDING, TargetingTactic.WOUNDED_HUNTER),
            new Spec("COMMAND_NEW_PLATE_BLEED", List.of("command_staff"), "NEW_PLATE", Position.FRONT, ActiveEnum.BLEEDING, TargetingTactic.WOUNDED_HUNTER),
            new Spec("COMMAND_PLATE_DOUBLE", List.of("command_staff"), "PLATE", Position.FRONT, ActiveEnum.DOUBLE_ATTACK, TargetingTactic.WOUNDED_HUNTER),
            new Spec("COMMAND_NEW_PLATE_DOUBLE", List.of("command_staff"), "NEW_PLATE", Position.FRONT, ActiveEnum.DOUBLE_ATTACK, TargetingTactic.WOUNDED_HUNTER),
            new Spec("COMMAND_CUIRASS_BLEED", List.of("command_staff"), "LEATHER", Position.FRONT, ActiveEnum.BLEEDING, TargetingTactic.WOUNDED_HUNTER),
            new Spec("COMMAND_DOUBLE", List.of("command_staff"), "LEATHER", Position.FRONT, ActiveEnum.DOUBLE_ATTACK, TargetingTactic.WOUNDED_HUNTER),
            new Spec("COMMAND_BLEED", List.of("command_staff"), "LEATHER", Position.FRONT, ActiveEnum.BLEEDING, TargetingTactic.WOUNDED_HUNTER),
            new Spec("COMMAND_TEMPO", List.of("command_staff"), "LEATHER", Position.FRONT, ActiveEnum.TEMPO_BREAK, TargetingTactic.INITIATIVE_INTERCEPTION),
            new Spec("STAFF_HEAL", List.of("staff", "orb"), "LEATHER", Position.FRONT, ActiveEnum.BERSERK, TargetingTactic.WOUNDED_HUNTER),
            new Spec("STAFF_ARCANE", List.of("staff", "orb"), "ARCANE", Position.FRONT, ActiveEnum.BERSERK, TargetingTactic.WOUNDED_HUNTER),
            new Spec("STAFF_NEW_ARCANE", List.of("staff", "orb"), "NEW_ARCANE", Position.FRONT, ActiveEnum.BERSERK, TargetingTactic.WOUNDED_HUNTER),
            new Spec("STAFF_DOUBLE", List.of("staff", "orb"), "LEATHER", Position.FRONT, ActiveEnum.DOUBLE_ATTACK, TargetingTactic.WOUNDED_HUNTER),
            new Spec("STAFF_BLEED", List.of("staff", "orb"), "LEATHER", Position.FRONT, ActiveEnum.BLEEDING, TargetingTactic.WOUNDED_HUNTER),
            new Spec("STAFF_TEMPO", List.of("staff", "orb"), "LEATHER", Position.FRONT, ActiveEnum.TEMPO_BREAK, TargetingTactic.INITIATIVE_INTERCEPTION),
            new Spec("HALBERD_BERSERK", List.of("halberd"), "LEATHER", Position.MID, ActiveEnum.BERSERK, TargetingTactic.WOUNDED_HUNTER),
            new Spec("RITUAL_HEAL", List.of("ritual_staff"), "ARCANE", Position.BACK, ActiveEnum.ACCUMULATION, TargetingTactic.EXECUTIONER),
            new Spec("RITUAL_NEW_ARCANE", List.of("ritual_staff"), "NEW_ARCANE", Position.BACK, ActiveEnum.ACCUMULATION, TargetingTactic.EXECUTIONER),
            new Spec("SWORD_DIRK", List.of("sword", "dirk"), "LEATHER", Position.FRONT, ActiveEnum.DOUBLE_ATTACK, TargetingTactic.WOUNDED_HUNTER),
            new Spec("SWORD_DIRK_PLATE", List.of("sword", "dirk"), "PLATE", Position.FRONT, ActiveEnum.DOUBLE_ATTACK, TargetingTactic.WOUNDED_HUNTER),
            new Spec("SWORD_SHIELD_PLATE", List.of("sword", "shield"), "PLATE", Position.FRONT, ActiveEnum.BLEEDING, TargetingTactic.WOUNDED_HUNTER),
            new Spec("SWORD_SHIELD_NEW_PLATE", List.of("sword", "shield"), "NEW_PLATE", Position.FRONT, ActiveEnum.BLEEDING, TargetingTactic.WOUNDED_HUNTER),
            new Spec("BRUISER_PLATE", List.of("sledgehammer"), "PLATE", Position.FRONT, ActiveEnum.BERSERK, TargetingTactic.WOUNDED_HUNTER),
            new Spec("HALBERD_PLATE", List.of("halberd"), "PLATE", Position.MID, ActiveEnum.BERSERK, TargetingTactic.WOUNDED_HUNTER),
            new Spec("SWORD_SHIELD_DOUBLE", List.of("sword", "shield"), "LEATHER", Position.FRONT, ActiveEnum.DOUBLE_ATTACK, TargetingTactic.WOUNDED_HUNTER),
            new Spec("SWORD_SHIELD_BLEED", List.of("sword", "shield"), "LEATHER", Position.FRONT, ActiveEnum.BLEEDING, TargetingTactic.WOUNDED_HUNTER),
            new Spec("SWORD_SHIELD_CUIRASS_BLEED", List.of("sword", "shield"), "LEATHER", Position.FRONT, ActiveEnum.BLEEDING, TargetingTactic.WOUNDED_HUNTER),
            new Spec("RAPIER_SHIELD_BLEED", List.of("rapier", "shield"), "LEATHER", Position.FRONT, ActiveEnum.BLEEDING, TargetingTactic.WOUNDED_HUNTER),
            new Spec("RAPIER_SHIELD_PLATE_BLEED", List.of("rapier", "shield"), "PLATE", Position.FRONT, ActiveEnum.BLEEDING, TargetingTactic.WOUNDED_HUNTER),
            new Spec("TWOHAND_SWORD_PLATE", List.of("two_handed_sword"), "PLATE", Position.FRONT, ActiveEnum.BLEEDING, TargetingTactic.WOUNDED_HUNTER),
            new Spec("SWORD_SHIELD_HEAL", List.of("sword", "shield"), "LEATHER", Position.FRONT, ActiveEnum.BERSERK, TargetingTactic.WOUNDED_HUNTER),
            new Spec("RANGER_HEAL", List.of("longbow"), "PLATE", Position.BACK, ActiveEnum.DOUBLE_ATTACK, TargetingTactic.EXPLOIT_WEAKNESS),
            new Spec("MAGE_HEAL", List.of("battle_staff"), "ARCANE", Position.BACK, ActiveEnum.DOUBLE_ATTACK, TargetingTactic.EXPLOIT_WEAKNESS),
            new Spec("GUARD_HEAL", List.of("mace", "tower_shield"), "PLATE", Position.FRONT, ActiveEnum.GUARD, TargetingTactic.THREAT),
            new Spec("TEMPO_HEAL", List.of("halberd"), "ARCANE", Position.MID, ActiveEnum.TEMPO_BREAK, TargetingTactic.INITIATIVE_INTERCEPTION),
            new Spec("KNOCK_HEAL", List.of("bow"), "CLOTH", Position.BACK, ActiveEnum.KNOCKBACK, TargetingTactic.EXPLOIT_WEAKNESS),
            new Spec("CHARGE_HEAL", List.of("crossbow"), "ARCANE", Position.BACK, ActiveEnum.ACCUMULATION, TargetingTactic.EXECUTIONER))) {
            final var armor = Step12RepresentativeBuilds.armor(spec.armor());
            final var a = new LinkedHashMap<String, Assignment>();
            final String carrier = spec.skill() == ActiveEnum.GUARD ? "tower_shield" : spec.hands().get(0);
            a.put(carrier, new Assignment(spec.skill(), rarity)); a.put(armor.get(0), new Assignment(ActiveEnum.SELF_HEAL, rarity));
            if (heavy) { a.put(armor.get(1), new Assignment(ActiveEnum.SELF_HEAL, rarity));
                a.put(armor.get(4), new Assignment(spec.code().equals("COMMAND_FEINT_HEAL") ? ActiveEnum.FEINT : ActiveEnum.THORNS, rarity)); }
            var build = Step12RepresentativeBuilds.hand(spec.code(), spec.hands(), spec.armor(), spec.position(), spec.tactic()).withAssignments(a);
            if (spec.code().contains("CUIRASS")) build = build.replaceItem("breastplate", "cuirass");
            out.add(build);
        }
        return List.copyOf(out);
    }

    private static List<Gear> repeat(Build b, int n) {
        final var out = new ArrayList<Gear>();
        final String guard = System.getProperty("audit.guard", "NONE");
        if (n > 1 && !guard.equals("NONE")) {
            var g = Step12RepresentativeBuilds.hand("FIXED_GUARD_" + guard, List.of("mace", "tower_shield"),
                "PLATE", Position.FRONT, TargetingTactic.THREAT);
            if (!guard.equals("COMMON")) g = g.withAssignments(Map.of("tower_shield", new Assignment(ActiveEnum.GUARD, ItemRarity.valueOf(guard))));
            out.add(new Gear(g, CATALOG));
        }
        while (out.size() < n) out.add(new Gear(b, CATALOG));
        return List.copyOf(out);
    }
    private static List<Integer> ints(String key, String defaults) {
        return Arrays.stream(System.getProperty(key, defaults).split(",")).map(Integer::valueOf).toList();
    }
    private static long cost(List<Gear> team, int level, Step123ReferenceEconomy economy) {
        return economy.nominalGold(team.stream().flatMap(g -> g.items(level).stream()).toList());
    }
    private static List<Object> manifest(List<Gear> team, int level, Step123ReferenceEconomy economy) {
        return team.stream().map(g -> (Object) map("build", g.build(), "catalog", g.catalog(),
            "nominalGold", economy.nominalGold(g.items(level)), "items", g.items(level).stream().map(i -> map(
                "code", i.object().code(), "rarity", i.rarity(), "enhanceLevel", i.enhanceLevel(),
                "skill", i.modifier().map(m -> m.activeEnum().name()).orElse("NONE"),
                "skillPoints", i.modifier().isPresent() ? i.skillPoints() : 0,
                "health", i.health(), "speed", i.speed(), "attacks", i.itemAttacks(),
                "defense", i.itemDefense().map(d -> map("type", d.defenseType(), "value", d.defense())).orElse(null))).toList())).toList();
    }
    private static Map<String, Object> provenance() throws Exception {
        final var result = new LinkedHashMap<String, Object>();
        for (final var type : List.of(Nov07ComparativeStudy.class, BattlePersonage.class, BattleContext.class,
            BattleTempoVersion.class, BattleActionLog.class, Step123BattleRunner.class,
            Step12RepresentativeBuilds.class, Step12RepresentativeBattleRunner.class,
            Step123CorrectionsStudy.class, Step123ReferenceEconomy.class, EquipmentCatalogVersion.class)) {
            final String resource = "/" + type.getName().replace('.', '/') + ".class";
            try (final var in = type.getResourceAsStream(resource)) {
                result.put(type.getName(), map("origin", type.getResource(resource).toString(),
                    "sha256", HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(in.readAllBytes()))));
            }
        }
        return result;
    }
    private static double score(String outcome) {
        return outcome.equals("WIN") ? 1 : outcome.equals("LOSS") ? 0 : .5;
    }
    private static Map<String, Double> pairInterval(List<Double> v) {
        final double mean = v.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
        final double delta = Math.sqrt(Math.log(40) / (2 * v.size()));
        return Map.of("low", Math.max(0, mean - delta), "high", Math.min(1, mean + delta));
    }
    private static Map<String, Object> stats(long[] v) {
        Arrays.sort(v);
        return map("mean", Arrays.stream(v).average().orElseThrow(),
            "median", (v[(v.length - 1) / 2] + v[v.length / 2]) / 2.0,
            "p95", v[(int) Math.ceil(v.length * .95) - 1], "min", v[0], "max", v[v.length - 1]);
    }
    private static long mix(long root, int pair) {
        long x = root + 0x9e3779b97f4a7c15L * (pair + 1L);
        x = (x ^ x >>> 30) * 0xbf58476d1ce4e5b9L;
        x = (x ^ x >>> 27) * 0x94d049bb133111ebL;
        return x ^ x >>> 31;
    }
    private static Map<String, Object> map(Object... pairs) {
        final var result = new LinkedHashMap<String, Object>();
        for (int i = 0; i < pairs.length; i += 2) result.put((String) pairs[i], pairs[i + 1]);
        return result;
    }
}
