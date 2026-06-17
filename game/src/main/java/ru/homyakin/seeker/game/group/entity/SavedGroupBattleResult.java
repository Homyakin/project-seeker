package ru.homyakin.seeker.game.group.entity;

import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.battle.GroupBattleStats;

public record SavedGroupBattleResult(
    GroupId groupId,
    long launchedEventId,
    GroupBattleStats stats
) {
}
