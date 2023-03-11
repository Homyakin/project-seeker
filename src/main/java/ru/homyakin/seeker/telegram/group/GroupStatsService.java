package ru.homyakin.seeker.telegram.group;

import java.util.Optional;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.telegram.group.database.GroupStatsDao;
import ru.homyakin.seeker.telegram.group.models.GroupStats;

@Service
public class GroupStatsService {
    private final GroupStatsDao groupStatsDao;

    public GroupStatsService(GroupStatsDao groupStatsDao) {
        this.groupStatsDao = groupStatsDao;
    }

    public void create(long groupId) {
        groupStatsDao.create(groupId);
    }

    public Optional<GroupStats> findById(long groupId) {
        return groupStatsDao.getById(groupId);
    }

    public void increaseRaidsComplete(long groupId, int amount) {
        groupStatsDao.increaseRaidsComplete(groupId, amount);
    }

    public void increaseDuelsComplete(long groupId, int amount) {
        groupStatsDao.increaseDuelsComplete(groupId, amount);
    }

    public void increaseTavernMoneySpent(long groupId, long amount) {
        groupStatsDao.increaseTavernMoneySpent(groupId, amount);
    }
}
