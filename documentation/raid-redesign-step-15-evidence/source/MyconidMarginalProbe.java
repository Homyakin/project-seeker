package ru.homyakin.seeker.game.battle;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;
import ru.homyakin.seeker.game.battle.PveAdaptationStudy.Cell;
import ru.homyakin.seeker.game.battle.PveAdaptationStudy.Mode;
import ru.homyakin.seeker.game.battle.Step123ReferenceTeams.Tier;
import ru.homyakin.seeker.game.event.raid.models.RaidType;
import ru.homyakin.seeker.utils.RandomUtils;

public final class MyconidMarginalProbe {
    public static void main(String[] args) throws Exception {
        final var output = Path.of(args[0]);
        Files.createDirectory(output);
        final int pairs = Integer.getInteger("probe.pairs", 20);
        try (var raw = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(
            output.resolve("actors.jsonl.gz"), StandardOpenOption.CREATE_NEW)), StandardCharsets.UTF_8))) {
            for (final var tier : List.of(Tier.MID, Tier.STRONG)) {
                final int groupId = tier == Tier.MID ? 27 : 33;
                for (int pair = 0; pair < pairs; pair++) {
                    final long generationSeed = 2_026_110_815_000L + 300_000_000L + (10_000L + groupId) * 1000 + pair;
                    final long battleSeed = 3_026_110_815_000L + 300_000_000L + (10_000L + groupId) * 1000 + pair;
                    for (final int size : List.of(3, 4)) {
                        for (final boolean reverse : List.of(false, true)) {
                            final var cell = new Cell(Mode.RAID, RaidType.MYCONID_COLONY.name(), "MAIN", tier, size, 25, false);
                            final var prepared = RandomUtils.withSeed(generationSeed, () -> PveAdaptationStudy.prepare(cell));
                            final var players = prepared.players();
                            final var enemies = prepared.generated().enemies();
                            final var labels = new LinkedHashMap<UUID, String>();
                            final var powers = new LinkedHashMap<UUID, Double>();
                            for (int i = 0; i < players.size(); i++) {
                                labels.put(players.get(i).id(), "P" + i + "_" + prepared.members().get(i).build());
                                powers.put(players.get(i).id(), players.get(i).legacyPower());
                            }
                            for (int i = 0; i < enemies.size(); i++) {
                                labels.put(enemies.get(i).id(), "E" + i + "_" + enemies.get(i).startPosition());
                                powers.put(enemies.get(i).id(), enemies.get(i).legacyPower());
                            }
                            final var result = RandomUtils.withSeed(battleSeed, () -> new Battle(battleSeed).process(
                                reverse ? players : enemies, reverse ? enemies : players, 2000, PveAdaptationStudy.TEMPO));
                            final var row = new LinkedHashMap<String, Object>();
                            row.put("tier", tier);
                            row.put("size", size);
                            row.put("pair", pair);
                            row.put("reverse", reverse);
                            row.put("generationSeed", generationSeed);
                            row.put("battleSeed", battleSeed);
                            row.put("calibration", prepared.generated().calibration());
                            row.put("outcome", players.stream().anyMatch(BattlePersonage::isAlive) ? "WIN" : "LOSS");
                            row.put("rounds", result.rounds());
                            final var actors = new ArrayList<Map<String, Object>>();
                            for (final var actor : labels.entrySet()) {
                                actors.add(actor(result, actor.getKey(), actor.getValue(), labels, powers.get(actor.getKey())));
                            }
                            row.put("actors", actors);
                            raw.write(PveAdaptationStudy.JSON.writeValueAsString(row));
                            raw.newLine();
                            if (pair < 2) {
                                final var full = new LinkedHashMap<>(row);
                                full.put("labels", labels);
                                full.put("result", result);
                                full.put("trace", result.actionLog().traceEvents());
                                Files.writeString(output.resolve(tier + "-N" + size + "-pair" + pair + "-" + reverse + ".json"),
                                    PveAdaptationStudy.JSON.writerWithDefaultPrettyPrinter().writeValueAsString(full),
                                    StandardOpenOption.CREATE_NEW);
                            }
                        }
                    }
                }
                System.out.println("complete " + tier + " pairs=" + pairs);
            }
        }
    }

    private static Map<String, Object> actor(
        BattleResult result, UUID id, String label, Map<UUID, String> labels, double power
    ) {
        final var row = new LinkedHashMap<String, Object>();
        row.put("label", label);
        row.put("power", power);
        row.put("init", result.initState().personagesById().get(id));
        row.put("stats", result.personageStats().get(id));
        final var distances = new TreeMap<Integer, Integer>();
        final var targets = new TreeMap<String, Integer>();
        final var originalTargets = new TreeMap<String, Integer>();
        final var lines = new TreeMap<Integer, Integer>();
        final var attacksAgainst = new TreeMap<String, Integer>();
        final var damageReceived = new TreeMap<String, Long>();
        final var damageDealt = new TreeMap<String, Long>();
        final var forcedBy = new TreeMap<String, Integer>();
        int turns = 0;
        int attempts = 0;
        int firstAttack = -1;
        int dodged = 0;
        int forwardMoves = 0;
        int forcedMoves = 0;
        int intercepts = 0;
        int delays = 0;
        long basis = 0;
        long normalDamage = 0;
        Integer defeatRound = null;
        for (final var trace : result.actionLog().traceEvents()) {
            if (trace instanceof BattleTraceEvent.TurnStarted turn && turn.personageId().equals(id)) {
                ++turns;
                lines.merge(turn.lineIndex(), 1, Integer::sum);
            }
            if (trace instanceof BattleTraceEvent.NormalAttackAttempt attack) {
                if (attack.attackerId().equals(id)) {
                    ++attempts;
                    if (firstAttack < 0) {
                        firstAttack = attack.round();
                    }
                    if (attack.dodged()) {
                        ++dodged;
                    }
                    basis += attack.savedBasis().values().stream().mapToLong(Integer::longValue).sum();
                    normalDamage += attack.normalDamage();
                    distances.merge(attack.distance(), 1, Integer::sum);
                    targets.merge(labels.get(attack.finalTargetId()), 1, Integer::sum);
                    originalTargets.merge(labels.get(attack.originalTargetId()), 1, Integer::sum);
                    damageDealt.merge(labels.get(attack.finalTargetId()), (long) attack.normalDamage(), Long::sum);
                }
                if (attack.finalTargetId().equals(id)) {
                    attacksAgainst.merge(labels.get(attack.attackerId()), 1, Integer::sum);
                    damageReceived.merge(labels.get(attack.attackerId()), (long) attack.normalDamage(), Long::sum);
                }
            }
        }
        for (final var event : result.actionLog().events()) {
            if (event instanceof BattleEvent.PersonageDefeated defeat && defeat.personageId().equals(id)) {
                defeatRound = defeat.round();
            }
            if (event instanceof BattleEvent.MovedTowardEnemy move && move.personageId().equals(id)) {
                ++forwardMoves;
            }
            if (event instanceof BattleEvent.PersonageForcedMove move && move.personageId().equals(id)) {
                ++forcedMoves;
                forcedBy.merge(labels.get(move.sourceId()), 1, Integer::sum);
            }
            if (event instanceof BattleEvent.AttackIntercepted interception && interception.interceptorId().equals(id)) {
                ++intercepts;
            }
            if (event instanceof BattleEvent.InitiativeDelayed delay && delay.targetId().equals(id)) {
                delays += delay.amount();
            }
            if (event instanceof BattleEvent.ScalingSkillDamage damage) {
                if (damage.targetId().equals(id)) {
                    damageReceived.merge(labels.get(damage.sourceId()), (long) damage.damageTaken(), Long::sum);
                }
                if (damage.sourceId().equals(id)) {
                    damageDealt.merge(labels.get(damage.targetId()), (long) damage.damageTaken(), Long::sum);
                }
            }
        }
        row.put("turns", turns);
        row.put("attempts", attempts);
        row.put("firstAttackRound", firstAttack);
        row.put("dodgedAttempts", dodged);
        row.put("attackBasisSum", basis);
        row.put("normalDamageSum", normalDamage);
        row.put("attemptDistances", distances);
        row.put("targets", targets);
        row.put("originalTargets", originalTargets);
        row.put("turnLines", lines);
        row.put("attacksAgainst", attacksAgainst);
        row.put("damageReceived", damageReceived);
        row.put("damageDealt", damageDealt);
        row.put("forwardMoves", forwardMoves);
        row.put("forcedMoves", forcedMoves);
        row.put("forcedBy", forcedBy);
        row.put("intercepts", intercepts);
        row.put("initiativeDelay", delays);
        row.put("defeatRound", defeatRound);
        return row;
    }
}
