package ru.homyakin.seeker.game.event.world_raid.action;

import ru.homyakin.seeker.common.models.GroupId;

public interface SendWorldRaidBattleToGroupCommand {
    void sendBattleToGroup(GroupId groupId);
}
