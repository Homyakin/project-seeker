package ru.homyakin.seeker.game.stats.entity;

import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.personage.models.PersonageId;

import java.util.Optional;

public interface GroupPersonageStatsStorage {
    void update(GroupPersonageStats groupPersonageStats);

    Optional<GroupPersonageStats> get(GroupId groupId, PersonageId personageId);
}
