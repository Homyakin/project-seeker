package ru.homyakin.seeker.game.stats.entity;

import ru.homyakin.seeker.common.models.GroupId;

public record GroupStats(
    GroupId groupId,
    int raidsComplete,
    int duelsComplete,
    long tavernMoneySpent
) {}
