package ru.homyakin.seeker.game.battle.v4;

import jakarta.annotation.Nullable;
import ru.homyakin.seeker.utils.RandomUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
                final var personage = mover.personage();
                if (!personage.isAlive()) {
                    continue;
                }

                final var target = randomAlivePersonage(mover.enemyAliveTeam());
                if (target == null) {
                    break; // enemy team is already dead
                }
                target.takeDamage(personage.getDamage());
                if (!target.isAlive()) {
                    mover.enemyAliveTeam().remove(target.id());
                }
            }
        }
        return new BattleResult(rounds, randomAlivePersonage(firstAliveTeam) != null);
    }

    @Nullable
    private static BattlePersonage randomAlivePersonage(Map<UUID, BattlePersonage> alivePersonages) {
        if (alivePersonages.isEmpty()) {
            return null;
        }

        final int random = RandomUtils.getInInterval(0, alivePersonages.size() - 1);
        int i = 0;
        for (final var entry : alivePersonages.entrySet()) {
            if (i == random) {
                return entry.getValue();
            }
            ++i;
        }
        return alivePersonages.values().stream().findFirst().orElse(null);
    }
}
