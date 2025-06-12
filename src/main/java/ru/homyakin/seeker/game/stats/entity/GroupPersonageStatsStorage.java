package ru.homyakin.seeker.game.stats.entity;

import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.season.entity.SeasonNumber;

import java.util.Optional;

public interface GroupPersonageStatsStorage {
    void add(GroupPersonageStats groupPersonageStats);

    Optional<GroupPersonageStats> get(GroupId groupId, PersonageId personageId, SeasonNumber seasonNumber);
}
