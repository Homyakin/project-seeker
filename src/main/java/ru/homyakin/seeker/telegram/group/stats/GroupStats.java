package ru.homyakin.seeker.telegram.group.stats;

import ru.homyakin.seeker.telegram.group.models.GroupId;

public record GroupStats(
    GroupId groupId,
    int raidsComplete,
    int duelsComplete,
    long tavernMoneySpent
) {}
