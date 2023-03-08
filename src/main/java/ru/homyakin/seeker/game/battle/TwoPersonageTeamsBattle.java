package ru.homyakin.seeker.game.battle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.utils.RandomUtils;

@Component
public class TwoPersonageTeamsBattle {

    public Result battle(ArrayList<BattlePersonage> firstTeam, ArrayList<BattlePersonage> secondTeam) {
        Collections.shuffle(firstTeam);
        Collections.shuffle(secondTeam);

        final var firstAliveTeam = firstTeam.stream()
            .collect(Collectors.toMap(BattlePersonage::id, it -> it));
        final Queue<Long> firstTeamAttackQueue = new LinkedList<>(firstTeam.stream().map(BattlePersonage::id).toList());

        final var secondAliveTeam = secondTeam.stream()
            .collect(Collectors.toMap(BattlePersonage::id, it -> it));
        final Queue<Long> secondTeamAttackQueue = new LinkedList<>(secondTeam.stream().map(BattlePersonage::id).toList());

        int teamTurn = RandomUtils.getInInterval(1, 2);
        while (!firstAliveTeam.isEmpty() && !secondAliveTeam.isEmpty()) {
            final var activeAliveTeam = teamTurn == 1 ? firstAliveTeam : secondAliveTeam;
            final var activeTeamAttackQueue = teamTurn == 1 ? firstTeamAttackQueue : secondTeamAttackQueue;
            final var enemyAliveTeam = teamTurn == 1 ? secondAliveTeam : firstAliveTeam;
            final var personage = attackPersonage(activeAliveTeam, activeTeamAttackQueue);
            final var targetPersonage = randomAlivePersonage(enemyAliveTeam);
            personage.dealDamageToPersonage(targetPersonage);
            activeTeamAttackQueue.add(personage.id());
            if (targetPersonage.isDead()) {
                enemyAliveTeam.remove(targetPersonage.id());
            }
            teamTurn = teamTurn == 1 ? 2 : 1;
        }

        if (firstAliveTeam.isEmpty()) {
            return new Result.SecondTeamWin();
        } else  {
            return new Result.FirstTeamWin();
        }
    }

    private BattlePersonage attackPersonage(Map<Long, BattlePersonage> alivePersonages, Queue<Long> attackQueue) {
        assert !attackQueue.isEmpty();
        Long personageId;
        do {
            personageId = attackQueue.poll();
        } while (!alivePersonages.containsKey(personageId));
        return alivePersonages.get(personageId);
    }

    private BattlePersonage randomAlivePersonage(Map<Long, BattlePersonage> alivePersonages) {
        final int random = RandomUtils.getInInterval(0, alivePersonages.size() - 1);
        int i = 0;
        for (final var entry : alivePersonages.entrySet()) {
            if (i == random) {
                return entry.getValue();
            }
            ++i;
        }
        return alivePersonages.values().stream().findFirst().orElseThrow();
    }

    public abstract static sealed class Result {

        public static final class FirstTeamWin extends Result {
        }

        public static final class SecondTeamWin extends Result {
        }
    }
}
