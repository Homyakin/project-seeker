package ru.homyakin.seeker.game.event.world_raid.entity.battle;

import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.battle.v3.GroupBattleStats;
import ru.homyakin.seeker.game.group.entity.Group;
import ru.homyakin.seeker.game.models.Money;

public record GroupWorldRaidBattleResult(
    Group group,
    GroupBattleStats stats,
    Money reward
) {
    public GroupId groupId() {
        return group.id();
    }
}
