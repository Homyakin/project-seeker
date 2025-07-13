package ru.homyakin.seeker.game.battle.v4;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.utils.RandomUtils;
import ru.homyakin.seeker.utils.models.Pair;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class TwoPersonageTeamsBattle {
    private final RandomUtils randomUtils;

    public TwoPersonageTeamsBattle(RandomUtils randomUtils) {
        this.randomUtils = randomUtils;
    }

    public TwoTeamBattleResult battle(List<BattlePersonage> firstTeam, List<BattlePersonage> secondTeam) {
        return new TwoTeamBattleResult(
            process(firstTeam, secondTeam)
        );
    }

    private TwoTeamBattleWinner process(List<BattlePersonage> firstTeam, List<BattlePersonage> secondTeam) {
        firstTeam = RandomUtils.shuffle(firstTeam);
        secondTeam = RandomUtils.shuffle(secondTeam);

        final var firstAliveTeam = firstTeam.stream()
            .collect(Collectors.toMap(BattlePersonage::id, it -> it));
        final var secondAliveTeam = secondTeam.stream()
            .collect(Collectors.toMap(BattlePersonage::id, it -> it));
        
        final var speedQueue = createSpeedBasedQueue(firstTeam, secondTeam);

        while (!firstAliveTeam.isEmpty() && !secondAliveTeam.isEmpty()) {
            final var nextAttacker = getNextAttacker(speedQueue, firstAliveTeam, secondAliveTeam);

            final var enemyTeam = nextAttacker.second() ? secondAliveTeam : firstAliveTeam;
            
            final var targetPersonage = randomAlivePersonage(enemyTeam);
            nextAttacker.first().dealDamageToPersonage(targetPersonage);
            
            if (targetPersonage.isDead()) {
                enemyTeam.remove(targetPersonage.id());
            }
        }

        if (firstAliveTeam.isEmpty()) {
            return TwoTeamBattleWinner.SECOND_TEAM;
        } else {
            return TwoTeamBattleWinner.FIRST_TEAM;
        }
    }

    private LinkedList<SpeedQueueEntry> createSpeedBasedQueue(List<BattlePersonage> firstTeam, List<BattlePersonage> secondTeam) {
        List<SpeedQueueEntry> allPersonages = new ArrayList<>();
        for (final var personage : firstTeam) {
            allPersonages.add(new SpeedQueueEntry(new PersonageTeamKey(personage.id(), true), personage.speed()));
        }
        for (final var personage : secondTeam) {
            allPersonages.add(new SpeedQueueEntry(new PersonageTeamKey(personage.id(), false), personage.speed()));
        }
        allPersonages = RandomUtils.shuffle(allPersonages);

        final var queue = new LinkedList<>(allPersonages);
        queue.sort(Comparator.comparing(SpeedQueueEntry::currentSpeed));
        return queue;
    }

    private Pair<BattlePersonage, Boolean> getNextAttacker(
        LinkedList<SpeedQueueEntry> speedQueue,
        Map<Long, BattlePersonage> firstAliveTeam,
        Map<Long, BattlePersonage> secondAliveTeam
    ) {
        assert !speedQueue.isEmpty();

        BattlePersonage personage;
        SpeedQueueEntry nextEntry;
        do {
            nextEntry = speedQueue.poll();
            if (nextEntry.key.firstTeam()) {
                personage = firstAliveTeam.get(nextEntry.key.personageId());
            } else {
                personage = secondAliveTeam.get(nextEntry.key.personageId());
            }
        } while (personage.isDead());

        // Reduce all other personages' speed by the attacker's speed
        for (final var entry : speedQueue) {
            entry.currentSpeed -= nextEntry.currentSpeed;
        }

        // Insert personage back into queue in correct position based on speed
        nextEntry.currentSpeed = personage.speed();
        insertInCorrectPosition(speedQueue, nextEntry);
        
        return Pair.of(personage, nextEntry.key.firstTeam());
    }

    private void insertInCorrectPosition(LinkedList<SpeedQueueEntry> speedQueue, SpeedQueueEntry newEntry) {
        int insertIndex = 0;
        for (int i = 0; i < speedQueue.size(); i++) {
            if (speedQueue.get(i).currentSpeed() > newEntry.currentSpeed()) {
                insertIndex = i;
                break;
            }
            insertIndex = i + 1;
        }

        speedQueue.add(insertIndex, newEntry);
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

    private static class SpeedQueueEntry {
        private final PersonageTeamKey key;
        private int currentSpeed;

        public SpeedQueueEntry(PersonageTeamKey key, int currentSpeed) {
            this.key = key;
            this.currentSpeed = currentSpeed;
        }

        public int currentSpeed() {
            return currentSpeed;
        }
    }

    public record PersonageTeamKey(Long personageId, boolean firstTeam) {
    }
}
