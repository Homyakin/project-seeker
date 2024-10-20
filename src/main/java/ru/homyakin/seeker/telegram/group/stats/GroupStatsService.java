package ru.homyakin.seeker.telegram.group.stats;

import java.util.Optional;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.event.models.EventResult;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;

@Service
public class GroupStatsService {
    private final GroupStatsDao groupStatsDao;
    private final GroupPersonageStatsService groupPersonageStatsService;

    public GroupStatsService(GroupStatsDao groupStatsDao, GroupPersonageStatsService groupPersonageStatsService) {
        this.groupStatsDao = groupStatsDao;
        this.groupPersonageStatsService = groupPersonageStatsService;
    }

    public void create(GroupTgId groupId) {
        groupStatsDao.create(groupId);
    }

    public Optional<GroupStats> findById(GroupTgId groupId) {
        return groupStatsDao.getById(groupId);
    }

    public void updateRaidStats(GroupTgId groupId, EventResult.RaidResult.Completed raidResult) {
        if (raidResult.status() == EventResult.RaidResult.Completed.Status.SUCCESS) {
            groupStatsDao.increaseRaidsComplete(groupId, 1);
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

    public void increaseDuelsComplete(GroupTgId groupId, PersonageId winner, PersonageId loser) {
        groupStatsDao.increaseDuelsComplete(groupId, 1);
        groupPersonageStatsService.addWinDuel(groupId, winner);
        groupPersonageStatsService.addLoseDuel(groupId, loser);
    }

    public void increaseTavernMoneySpent(GroupTgId groupId, PersonageId personageId, Money money) {
        groupStatsDao.increaseTavernMoneySpent(groupId, money.value());
        groupPersonageStatsService.increaseTavernMoneySpent(groupId, personageId, money);
    }
}
