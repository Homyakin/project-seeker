package ru.homyakin.seeker.game.stats.action;

import org.springframework.stereotype.Service;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.event.models.EventResult;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.season.action.SeasonService;
import ru.homyakin.seeker.game.stats.entity.GroupStats;
import ru.homyakin.seeker.game.stats.entity.GroupStatsStorage;

import java.util.Optional;

@Service
public class GroupStatsService {
    private final GroupStatsStorage storage;
    private final SeasonService seasonService;
    private final GroupPersonageStatsService groupPersonageStatsService;

    public GroupStatsService(
        GroupStatsStorage storage,
        SeasonService seasonService,
        GroupPersonageStatsService groupPersonageStatsService
    ) {
        this.storage = storage;
        this.seasonService = seasonService;
        this.groupPersonageStatsService = groupPersonageStatsService;
    }

    public Optional<GroupStats> getForCurrentSeason(GroupId groupId) {
        final var season = seasonService.currentSeason();
        return storage.get(groupId, season);
    }

    public void updateRaidStats(GroupId groupId, EventResult.RaidResult.Completed raidResult) {
        final var season = seasonService.currentSeason();
        if (raidResult.status() == EventResult.RaidResult.Completed.Status.SUCCESS) {
            storage.add(GroupStats.withSuccessRaid(season, groupId));
        } else {
            storage.add(GroupStats.withFailedRaid(season, groupId));
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

    public void addFailedWoldRaid(GroupId groupId) {
        final var season = seasonService.currentSeason();
        storage.add(GroupStats.withFailedWorldRaid(season, groupId));
    }

    public void addSuccessWoldRaid(GroupId groupId) {
        final var season = seasonService.currentSeason();
        storage.add(GroupStats.withSuccessWorldRaid(season, groupId));
    }

    public void increaseDuelsComplete(GroupId groupId, PersonageId winner, PersonageId loser) {
        final var season = seasonService.currentSeason();
        storage.add(GroupStats.withCompleteDuel(season, groupId));
        groupPersonageStatsService.addWinDuel(groupId, winner);
        groupPersonageStatsService.addLoseDuel(groupId, loser);
    }

    public void increaseTavernMoneySpent(GroupId groupId, PersonageId personageId, Money money) {
        final var season = seasonService.currentSeason();
        storage.add(GroupStats.withSpentTavernMoney(season, groupId, money));
        groupPersonageStatsService.increaseTavernMoneySpent(groupId, personageId, money);
    }
}
