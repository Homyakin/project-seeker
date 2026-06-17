package ru.homyakin.seeker.game.event.world_raid.entity.battle;

import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.battle.GroupBattleStats;
import ru.homyakin.seeker.game.group.entity.Group;

public record GroupWorldRaidBattleResult(
    Group group,
    GroupBattleStats stats
) {
    public GroupId groupId() {
        return group.id();
    }
}
