package ru.homyakin.seeker.game.stats.entity;

import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.season.entity.SeasonNumber;

import java.util.Optional;

public interface GroupStatsStorage {
    Optional<GroupStats> get(GroupId groupId, SeasonNumber seasonNumber);

    void add(GroupStats stats);
}
