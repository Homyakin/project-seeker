package ru.homyakin.seeker.telegram.group.stats;

import java.util.Optional;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.telegram.group.models.GroupId;

@Service
public class GroupPersonageStatsService {
    private final GroupPersonageStatsDao groupPersonageStatsDao;

    public GroupPersonageStatsService(GroupPersonageStatsDao groupPersonageStatsDao) {
        this.groupPersonageStatsDao = groupPersonageStatsDao;
    }

    public GroupPersonageStats getOrCreate(GroupId groupId, PersonageId personageId) {
        return get(groupId, personageId)
            .orElseGet(() -> {
                create(groupId, personageId);
                return get(groupId, personageId).orElseThrow();
            });
    }

    public void create(GroupId groupId, PersonageId personageId) {
        groupPersonageStatsDao.create(groupId, personageId);
    }

    private Optional<GroupPersonageStats> get(GroupId groupId, PersonageId personageId) {
        return groupPersonageStatsDao.get(groupId, personageId);
    }

    public void addWinDuel(GroupId groupId, PersonageId personageId) {
        groupPersonageStatsDao.update(getOrCreate(groupId, personageId).addWinDuel());
    }

    public void addLoseDuel(GroupId groupId, PersonageId personageId) {
        groupPersonageStatsDao.update(getOrCreate(groupId, personageId).addLoseDuel());
    }

    public void addSuccessRaid(GroupId groupId, PersonageId personageId) {
        groupPersonageStatsDao.update(getOrCreate(groupId, personageId).addSuccessRaid());
    }

    public void addFailedRaid(GroupId groupId, PersonageId personageId) {
        groupPersonageStatsDao.update(getOrCreate(groupId, personageId).addFailedRaid());
    }

    public void addSpinWin(GroupId groupId, PersonageId personageId) {
        groupPersonageStatsDao.update(getOrCreate(groupId, personageId).addSpinWin());
    }

    public void increaseTavernMoneySpent(GroupId groupId, PersonageId personageId, Money money) {
        groupPersonageStatsDao.update(
            getOrCreate(groupId, personageId).increaseTavernMoneySpent(money.value())
        );
    }
}
