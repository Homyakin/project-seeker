package ru.homyakin.seeker.game.battle.two_team;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.utils.RandomUtils;

@Component
public class TwoPersonageTeamsBattle {

    public TwoTeamBattleResult battle(List<BattlePersonage> firstTeam, List<BattlePersonage> secondTeam) {
        return new TwoTeamBattleResult(
            process(firstTeam, secondTeam),
            firstTeam.stream().map(BattlePersonage::toResult).toList(),
            secondTeam.stream().map(BattlePersonage::toResult).toList()
        );
    }

    private TwoTeamBattleWinner process(List<BattlePersonage> firstTeam, List<BattlePersonage> secondTeam) {
        firstTeam = RandomUtils.shuffle(firstTeam);
        secondTeam = RandomUtils.shuffle(secondTeam);

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
            final var personage = attackingPersonage(activeAliveTeam, activeTeamAttackQueue);
            final var targetPersonage = randomAlivePersonage(enemyAliveTeam);
            personage.dealDamageToPersonage(targetPersonage);
            activeTeamAttackQueue.add(personage.id());
            if (targetPersonage.isDead()) {
                enemyAliveTeam.remove(targetPersonage.id());
            }
            teamTurn = teamTurn == 1 ? 2 : 1;
        }

        if (firstAliveTeam.isEmpty()) {
            return TwoTeamBattleWinner.SECOND_TEAM;
        } else  {
            return TwoTeamBattleWinner.FIRST_TEAM;
        }
    }

    private BattlePersonage attackingPersonage(Map<Long, BattlePersonage> alivePersonages, Queue<Long> attackQueue) {
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
}
