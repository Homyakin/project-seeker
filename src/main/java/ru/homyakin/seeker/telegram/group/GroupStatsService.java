package ru.homyakin.seeker.telegram.group;

import java.util.Optional;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.telegram.group.database.GroupStatsDao;
import ru.homyakin.seeker.telegram.group.models.GroupId;
import ru.homyakin.seeker.telegram.group.models.GroupStats;

@Service
public class GroupStatsService {
    private final GroupStatsDao groupStatsDao;

    public GroupStatsService(GroupStatsDao groupStatsDao) {
        this.groupStatsDao = groupStatsDao;
    }

    public void create(GroupId groupId) {
        groupStatsDao.create(groupId);
    }

    public Optional<GroupStats> findById(GroupId groupId) {
        return groupStatsDao.getById(groupId);
    }

    public void increaseRaidsComplete(GroupId groupId, int amount) {
        groupStatsDao.increaseRaidsComplete(groupId, amount);
    }

    public void increaseDuelsComplete(GroupId groupId, int amount) {
        groupStatsDao.increaseDuelsComplete(groupId, amount);
    }

    public void increaseTavernMoneySpent(GroupId groupId, Money money) {
        groupStatsDao.increaseTavernMoneySpent(groupId, money.value());
    }
}
