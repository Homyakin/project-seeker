package ru.homyakin.seeker.game.stats.entity;

import ru.homyakin.seeker.common.models.GroupId;

import java.util.Optional;

public interface GroupStatsStorage {
    Optional<GroupStats> get(GroupId groupId);

    void increaseRaidsComplete(GroupId groupId, int count);

    void increaseDuelsComplete(GroupId groupId, int count);

    void increaseTavernMoneySpent(GroupId groupId, long moneySpent);
}
