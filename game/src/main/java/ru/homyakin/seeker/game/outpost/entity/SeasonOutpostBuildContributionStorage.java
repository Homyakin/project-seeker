package ru.homyakin.seeker.game.outpost.entity;

import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.season.entity.SeasonNumber;

public interface SeasonOutpostBuildContributionStorage {
    void add(
        SeasonNumber seasonNumber,
        GroupId groupId,
        Building building,
        int targetLevel,
        PersonageId personageId,
        int materialsDelta
    );
}
