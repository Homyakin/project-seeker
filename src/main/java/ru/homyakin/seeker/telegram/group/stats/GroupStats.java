package ru.homyakin.seeker.telegram.group.stats;

import ru.homyakin.seeker.telegram.group.models.GroupTgId;

public record GroupStats(
    GroupTgId groupId,
    int raidsComplete,
    int duelsComplete,
    long tavernMoneySpent
) {}
