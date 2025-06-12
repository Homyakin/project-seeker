package ru.homyakin.seeker.game.stats.action;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.season.action.SeasonService;
import ru.homyakin.seeker.game.stats.entity.AddPersonageStats;
import ru.homyakin.seeker.game.stats.entity.PersonageStats;
import ru.homyakin.seeker.game.stats.entity.PersonageStatsStorage;

import java.util.Optional;

@Component
public class PersonageStatsService {
    private final PersonageStatsStorage storage;
    private final SeasonService seasonService;

    public PersonageStatsService(
        PersonageStatsStorage storage,
        SeasonService seasonService
    ) {
        this.storage = storage;
        this.seasonService = seasonService;
    }

    public Optional<PersonageStats> getForCurrentSeason(PersonageId personageId) {
        final var season = seasonService.currentSeason();
        return storage.get(personageId, season);
    }

    public void addSuccessWorldRaid(PersonageId personageId) {
        final var season = seasonService.currentSeason();
        storage.add(AddPersonageStats.withSuccessWorldRaid(season, personageId));
    }

    public void addFailedWorldRaid(PersonageId personageId) {
        final var season = seasonService.currentSeason();
        storage.add(AddPersonageStats.withFailedWorldRaid(season, personageId));
    }

    public void addSuccessQuest(PersonageId personageId) {
        final var season = seasonService.currentSeason();
        storage.add(AddPersonageStats.withSuccessQuest(season, personageId));
    }

    public void addFailedQuest(PersonageId personageId) {
        final var season = seasonService.currentSeason();
        storage.add(AddPersonageStats.withFailedQuest(season, personageId));
    }

    public void addQuests(PersonageId personageId, int success, int total) {
        final var season = seasonService.currentSeason();
        storage.add(AddPersonageStats.withQuests(season, personageId, success, total));
    }
}
