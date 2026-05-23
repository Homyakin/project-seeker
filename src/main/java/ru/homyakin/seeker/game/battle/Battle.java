package ru.homyakin.seeker.game.battle;

import ru.homyakin.seeker.utils.RandomUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Battle {
    private record Mover(BattlePersonage personage, Map<UUID, BattlePersonage> enemyAliveTeam) { }

    public BattleResult process(List<BattlePersonage> firstTeam, List<BattlePersonage> secondTeam) {
        final var battleMap = new BattleContext(firstTeam, secondTeam);
        final var initState = captureInitState(battleMap, firstTeam, secondTeam);
        final var actionLog = new BattleActionLog();

        final var firstAliveTeam = battleMap.firstAliveTeam();
        final var secondAliveTeam = battleMap.secondAliveTeam();

        int rounds = 0;
        while (!firstAliveTeam.isEmpty() && !secondAliveTeam.isEmpty()) {
            ++rounds;
            actionLog.add(new BattleEvent.RoundStarted(rounds));
            final var movers = new ArrayList<Mover>();

            for (final var p : firstAliveTeam.values()) {
                if (p.tick(actionLog, rounds)) {
                    movers.add(new Mover(p, secondAliveTeam));
                }
            }
            for (final var p : secondAliveTeam.values()) {
                if (p.tick(actionLog, rounds)) {
                    movers.add(new Mover(p, firstAliveTeam));
                }
            }

            for (final var mover : RandomUtils.shuffle(movers)) {
                if (mover.enemyAliveTeam().isEmpty()) {
                    break;
                }
                if (mover.personage().move(battleMap, actionLog, rounds)) {
                    break;
                }
                pruneDead(firstAliveTeam, secondAliveTeam);
            }
        }
        final var personageStats = new HashMap<UUID, BattlePersonageStats>();
        Stream.concat(firstTeam.stream(), secondTeam.stream())
            .forEach(p -> personageStats.put(p.id(), p.battlePersonageStats()));

        return new BattleResult(
            initState,
            actionLog,
            rounds,
            firstAliveTeam.values().stream().anyMatch(BattlePersonage::isAlive),
            personageStats
        );
    }

    private static void pruneDead(
        Map<UUID, BattlePersonage> firstAliveTeam,
        Map<UUID, BattlePersonage> secondAliveTeam
    ) {
        firstAliveTeam.entrySet().removeIf(entry -> !entry.getValue().isAlive());
        secondAliveTeam.entrySet().removeIf(entry -> !entry.getValue().isAlive());
    }

    private static BattleInitState captureInitState(
        BattleContext battleContext,
        List<BattlePersonage> firstTeam,
        List<BattlePersonage> secondTeam
    ) {
        final Set<UUID> firstTeamIds = firstTeam.stream().map(BattlePersonage::id).collect(Collectors.toSet());
        final var lines = battleContext.lines();
        final var lineSnapshots = new ArrayList<BattleLineInitSnapshot>();
        for (int i = 0; i < lines.size(); i++) {
            final var line = lines.get(i);
            final var ids = line.battlePersonages().keySet().stream().sorted().toList();
            lineSnapshots.add(new BattleLineInitSnapshot(i, line.firstTeam(), line.position(), ids));
        }
        final var personagesById = new HashMap<UUID, BattlePersonageInitSnapshot>();
        Stream.concat(firstTeam.stream(), secondTeam.stream()).forEach(p ->
            personagesById.put(
                p.id(),
                new BattlePersonageInitSnapshot(
                    p.id(),
                    firstTeamIds.contains(p.id()),
                    p.health(),
                    p.currentPosition(),
                    p.advanceDirection(),
                    p.initiative(),
                    p.initiativeGauge(),
                    p.range(),
                    p.totalThreat()
                )
            )
        );
        return new BattleInitState(List.copyOf(lineSnapshots), Map.copyOf(personagesById));
    }
}
