package ru.homyakin.seeker.game.group.entity;

import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.battle.v3.GroupBattleStats;
import ru.homyakin.seeker.game.models.Money;

public record SavedGroupBattleResult(
    GroupId groupId,
    long launchedEventId,
    GroupBattleStats stats,
    Money reward
) {
}
