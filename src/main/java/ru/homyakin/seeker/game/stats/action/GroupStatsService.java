package ru.homyakin.seeker.game.stats.action;

import org.springframework.stereotype.Service;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.event.models.EventResult;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.stats.entity.GroupStats;
import ru.homyakin.seeker.game.stats.entity.GroupStatsStorage;

@Service
public class GroupStatsService {
    private final GroupStatsStorage storage;
    private final GroupPersonageStatsService groupPersonageStatsService;

    public GroupStatsService(GroupStatsStorage storage, GroupPersonageStatsService groupPersonageStatsService) {
        this.storage = storage;
        this.groupPersonageStatsService = groupPersonageStatsService;
    }

    public GroupStats get(GroupId groupId) {
        return storage.get(groupId).orElseThrow(() -> new IllegalArgumentException("Group not found"));
    }

    public void updateRaidStats(GroupId groupId, EventResult.RaidResult.Completed raidResult) {
        if (raidResult.status() == EventResult.RaidResult.Completed.Status.SUCCESS) {
            storage.increaseRaidsComplete(groupId, 1);
        }
        raidResult.personageResults().forEach(
            personageResult -> {
                if (raidResult.status() == EventResult.RaidResult.Completed.Status.SUCCESS) {
                    groupPersonageStatsService.addSuccessRaid(groupId, personageResult.participant().personage().id());
                } else {
                    groupPersonageStatsService.addFailedRaid(groupId, personageResult.participant().personage().id());
                }
            }
        );
    }

    public void increaseDuelsComplete(GroupId groupId, PersonageId winner, PersonageId loser) {
        storage.increaseDuelsComplete(groupId, 1);
        groupPersonageStatsService.addWinDuel(groupId, winner);
        groupPersonageStatsService.addLoseDuel(groupId, loser);
    }

    public void increaseTavernMoneySpent(GroupId groupId, PersonageId personageId, Money money) {
        storage.increaseTavernMoneySpent(groupId, money.value());
        groupPersonageStatsService.increaseTavernMoneySpent(groupId, personageId, money);
    }
}
