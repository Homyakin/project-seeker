package ru.homyakin.seeker.telegram.group.stats;

import java.util.Optional;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.event.raid.models.RaidResult;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.telegram.group.models.GroupId;

@Service
public class GroupStatsService {
    private final GroupStatsDao groupStatsDao;
    private final GroupPersonageStatsService groupPersonageStatsService;

    public GroupStatsService(GroupStatsDao groupStatsDao, GroupPersonageStatsService groupPersonageStatsService) {
        this.groupStatsDao = groupStatsDao;
        this.groupPersonageStatsService = groupPersonageStatsService;
    }

    public void create(GroupId groupId) {
        groupStatsDao.create(groupId);
    }

    public Optional<GroupStats> findById(GroupId groupId) {
        return groupStatsDao.getById(groupId);
    }

    public void updateRaidStats(GroupId groupId, RaidResult raidResult) {
        if (raidResult.isSuccess()) {
            groupStatsDao.increaseRaidsComplete(groupId, 1);
        }
        raidResult.personageResults().forEach(
            personageResult -> {
                if (raidResult.isSuccess()) {
                    groupPersonageStatsService.addSuccessRaid(groupId, personageResult.personage().id());
                } else {
                    groupPersonageStatsService.addFailedRaid(groupId, personageResult.personage().id());
                }
            }
        );
    }

    public void increaseDuelsComplete(GroupId groupId, PersonageId winner, PersonageId loser) {
        groupStatsDao.increaseDuelsComplete(groupId, 1);
        groupPersonageStatsService.addWinDuel(groupId, winner);
        groupPersonageStatsService.addLoseDuel(groupId, loser);
    }

    public void increaseTavernMoneySpent(GroupId groupId, PersonageId personageId, Money money) {
        groupStatsDao.increaseTavernMoneySpent(groupId, money.value());
        groupPersonageStatsService.increaseTavernMoneySpent(groupId, personageId, money);
    }
}
