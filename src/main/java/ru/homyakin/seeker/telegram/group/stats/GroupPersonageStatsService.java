package ru.homyakin.seeker.telegram.group.stats;

import java.util.Optional;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.action.personage.SpinStats;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;

@Service
public class GroupPersonageStatsService implements SpinStats {
    private final GroupPersonageStatsDao groupPersonageStatsDao;

    public GroupPersonageStatsService(GroupPersonageStatsDao groupPersonageStatsDao) {
        this.groupPersonageStatsDao = groupPersonageStatsDao;
    }

    public GroupPersonageStats getOrCreate(GroupTgId groupId, PersonageId personageId) {
        return get(groupId, personageId)
            .orElseGet(() -> {
                create(groupId, personageId);
                return get(groupId, personageId).orElseThrow();
            });
    }

    public GroupPersonageStats getOrCreate(GroupId groupId, PersonageId personageId) {
        return groupPersonageStatsDao.get(groupId, personageId)
            .orElseGet(() -> {
                groupPersonageStatsDao.create(groupId, personageId);
                return groupPersonageStatsDao.get(groupId, personageId).orElseThrow();
            });
    }

    public void create(GroupTgId groupId, PersonageId personageId) {
        groupPersonageStatsDao.create(groupId, personageId);
    }

    private Optional<GroupPersonageStats> get(GroupTgId groupId, PersonageId personageId) {
        return groupPersonageStatsDao.get(groupId, personageId);
    }

    public void addWinDuel(GroupTgId groupId, PersonageId personageId) {
        groupPersonageStatsDao.update(getOrCreate(groupId, personageId).addWinDuel());
    }

    public void addLoseDuel(GroupTgId groupId, PersonageId personageId) {
        groupPersonageStatsDao.update(getOrCreate(groupId, personageId).addLoseDuel());
    }

    public void addSuccessRaid(GroupTgId groupId, PersonageId personageId) {
        groupPersonageStatsDao.update(getOrCreate(groupId, personageId).addSuccessRaid());
    }

    public void addFailedRaid(GroupTgId groupId, PersonageId personageId) {
        groupPersonageStatsDao.update(getOrCreate(groupId, personageId).addFailedRaid());
    }

    public void increaseTavernMoneySpent(GroupTgId groupId, PersonageId personageId, Money money) {
        groupPersonageStatsDao.update(
            getOrCreate(groupId, personageId).increaseTavernMoneySpent(money.value())
        );
    }

    @Override
    public void addPersonageSpinWin(GroupId groupId, PersonageId personageId) {
        groupPersonageStatsDao.update(getOrCreate(groupId, personageId).addSpinWin());
    }
}
