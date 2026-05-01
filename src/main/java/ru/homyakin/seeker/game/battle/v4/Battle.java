package ru.homyakin.seeker.game.battle.v4;

import ru.homyakin.seeker.utils.RandomUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Battle {
    private record Mover(BattlePersonage personage, Map<UUID, BattlePersonage> enemyAliveTeam) { }

    public BattleResult process(List<BattlePersonage> firstTeam, List<BattlePersonage> secondTeam) {
        final var firstAliveTeam = firstTeam.stream()
            .filter(BattlePersonage::isAlive)
            .collect(Collectors.toMap(BattlePersonage::id, it -> it));
        final var secondAliveTeam = secondTeam.stream()
            .filter(BattlePersonage::isAlive)
            .collect(Collectors.toMap(BattlePersonage::id, it -> it));

        int rounds = 0;
        while (!firstAliveTeam.isEmpty() && !secondAliveTeam.isEmpty()) {
            ++rounds;
            final var movers = new ArrayList<Mover>();

            for (final var p : firstAliveTeam.values()) {
                if (p.tick()) {
                    movers.add(new Mover(p, secondAliveTeam));
                }
            }
            for (final var p : secondAliveTeam.values()) {
                if (p.tick()) {
                    movers.add(new Mover(p, firstAliveTeam));
                }
            }

            for (final var mover : RandomUtils.shuffle(movers)) {
                if (mover.enemyAliveTeam().isEmpty()) {
                    break;
                }
                if (mover.personage().move(mover.enemyAliveTeam())) {
                    break;
                }
            }
        }
        final var personageStats = new HashMap<UUID, BattlePersonageStats>();
        Stream.concat(firstTeam.stream(), secondTeam.stream())
            .forEach(p -> personageStats.put(p.id(), p.battlePersonageStats()));

        return new BattleResult(
            rounds,
            BattlePersonage.randomAlivePersonage(firstAliveTeam) != null,
            Map.copyOf(personageStats)
        );
    }
}
