package ru.homyakin.seeker.game.stats.action;

import org.springframework.stereotype.Service;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.stats.entity.GroupPersonageClient;
import ru.homyakin.seeker.game.stats.entity.GroupPersonageStats;
import ru.homyakin.seeker.game.stats.entity.GroupPersonageStatsStorage;

@Service
public class GroupPersonageStatsService {
    private final GroupPersonageStatsStorage storage;
    private final GroupPersonageClient groupPersonageClient;

    public GroupPersonageStatsService(GroupPersonageStatsStorage storage, GroupPersonageClient groupPersonageClient) {
        this.storage = storage;
        this.groupPersonageClient = groupPersonageClient;
    }

    public GroupPersonageStats getOrCreate(GroupId groupId, PersonageId personageId) {
        return storage.get(groupId, personageId)
            .orElseGet(
                () -> {
                    groupPersonageClient.create(groupId, personageId);
                    return storage.get(groupId, personageId).orElseThrow();
                }
            );
    }

    public void addWinDuel(GroupId groupId, PersonageId personageId) {
        storage.update(getOrCreate(groupId, personageId).addWinDuel());
    }

    public void addLoseDuel(GroupId groupId, PersonageId personageId) {
        storage.update(getOrCreate(groupId, personageId).addLoseDuel());
    }

    public void addSuccessRaid(GroupId groupId, PersonageId personageId) {
        storage.update(getOrCreate(groupId, personageId).addSuccessRaid());
    }

    public void addFailedRaid(GroupId groupId, PersonageId personageId) {
        storage.update(getOrCreate(groupId, personageId).addFailedRaid());
    }

    public void increaseTavernMoneySpent(GroupId groupId, PersonageId personageId, Money money) {
        storage.update(
            getOrCreate(groupId, personageId).increaseTavernMoneySpent(money.value())
        );
    }

    public void addPersonageSpinWin(GroupId groupId, PersonageId personageId) {
        storage.update(getOrCreate(groupId, personageId).addSpinWin());
    }
}
