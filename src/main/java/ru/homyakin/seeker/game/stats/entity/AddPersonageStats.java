package ru.homyakin.seeker.game.stats.entity;

import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.season.entity.SeasonNumber;

public record AddPersonageStats(
    SeasonNumber seasonNumber,
    PersonageId personageId,
    int questsSuccess,
    int questsTotal,
    int worldRaidsSuccess,
    int worldRaidsTotal
) {
    public static AddPersonageStats withSuccessQuest(SeasonNumber seasonNumber, PersonageId personageId) {
        return new AddPersonageStats(
            seasonNumber,
            personageId,
            1,
            1,
            0,
            0
        );
    }

    public static AddPersonageStats withFailedQuest(SeasonNumber seasonNumber, PersonageId personageId) {
        return new AddPersonageStats(
            seasonNumber,
            personageId,
            0,
            1,
            0,
            0
        );
    }

    public static AddPersonageStats withQuests(
        SeasonNumber seasonNumber,
        PersonageId personageId,
        int questsSuccess,
        int questsTotal
    ) {
        return new AddPersonageStats(
            seasonNumber,
            personageId,
            questsSuccess,
            questsTotal,
            0,
            0
        );
    }

    public static AddPersonageStats withSuccessWorldRaid(SeasonNumber seasonNumber, PersonageId personageId) {
        return new AddPersonageStats(
            seasonNumber,
            personageId,
            0,
            0,
            1,
            1
        );
    }

    public static AddPersonageStats withFailedWorldRaid(SeasonNumber seasonNumber, PersonageId personageId) {
        return new AddPersonageStats(
            seasonNumber,
            personageId,
            0,
            0,
            0,
            1
        );
    }
}
