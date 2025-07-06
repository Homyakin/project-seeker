package ru.homyakin.seeker.game.stats.action;

import org.springframework.stereotype.Service;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.season.action.SeasonService;
import ru.homyakin.seeker.game.stats.entity.GroupPersonageStats;
import ru.homyakin.seeker.game.stats.entity.GroupPersonageStatsStorage;

import java.util.Optional;

@Service
public class GroupPersonageStatsService {
    private final GroupPersonageStatsStorage storage;
    private final SeasonService seasonService;

    public GroupPersonageStatsService(
        GroupPersonageStatsStorage storage,
        SeasonService seasonService
    ) {
        this.storage = storage;
        this.seasonService = seasonService;
    }

    public Optional<GroupPersonageStats> getForCurrentSeason(GroupId groupId, PersonageId personageId) {
        final var season = seasonService.currentSeason();
        return storage.get(groupId, personageId, season);
    }

    public void addWinDuel(GroupId groupId, PersonageId personageId) {
        final var season = seasonService.currentSeason();
        storage.add(GroupPersonageStats.withWinDuel(season, groupId, personageId));
    }

    public void addLoseDuel(GroupId groupId, PersonageId personageId) {
        final var season = seasonService.currentSeason();
        storage.add(GroupPersonageStats.withLoseDuel(season, groupId, personageId));
    }

    public void addSuccessRaid(GroupId groupId, PersonageId personageId) {
        final var season = seasonService.currentSeason();
        storage.add(GroupPersonageStats.withSuccessRaid(season, groupId, personageId));
    }

    public void addFailedRaid(GroupId groupId, PersonageId personageId) {
        final var season = seasonService.currentSeason();
        storage.add(GroupPersonageStats.withFailedRaid(season, groupId, personageId));
    }

    public void addSpentTavernMoney(GroupId groupId, PersonageId personageId, Money money) {
        final var season = seasonService.currentSeason();
        storage.add(GroupPersonageStats.withSpentTavernMoney(season, groupId, personageId, money));
    }

    public void addWorkerOfDay(GroupId groupId, PersonageId personageId) {
        final var season = seasonService.currentSeason();
        storage.add(GroupPersonageStats.withWorkerOfDay(season, groupId, personageId));
    }

    public void addDonateMoney(GroupId groupId, PersonageId personageId, Money money) {
        final var season = seasonService.currentSeason();
        storage.add(GroupPersonageStats.withDonateMoney(season, groupId, personageId, money));
    }

    public void addGiveMoney(GroupId groupId, PersonageId personageId, Money money) {
        final var season = seasonService.currentSeason();
        storage.add(GroupPersonageStats.withGiveMoney(season, groupId, personageId, money));
    }
}
