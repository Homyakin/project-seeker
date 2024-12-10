package ru.homyakin.seeker.game.stats.action;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.stats.entity.PersonageStats;
import ru.homyakin.seeker.game.stats.entity.PersonageStatsStorage;

@Component
public class GetPersonageStatsCommand {
    private final PersonageStatsStorage personageStatsStorage;

    public GetPersonageStatsCommand(PersonageStatsStorage personageStatsStorage) {
        this.personageStatsStorage = personageStatsStorage;
    }

    public PersonageStats execute(PersonageId personageId) {
        return personageStatsStorage.get(personageId);
    }
}
