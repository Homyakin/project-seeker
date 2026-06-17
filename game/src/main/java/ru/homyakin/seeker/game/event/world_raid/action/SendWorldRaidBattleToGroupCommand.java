package ru.homyakin.seeker.game.event.world_raid.action;

import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.event.launched.LaunchedEvent;

public interface SendWorldRaidBattleToGroupCommand {
    void sendBattleToGroup(GroupId groupId, LaunchedEvent event);
}
