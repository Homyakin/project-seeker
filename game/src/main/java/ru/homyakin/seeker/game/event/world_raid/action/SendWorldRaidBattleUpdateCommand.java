package ru.homyakin.seeker.game.event.world_raid.action;

import ru.homyakin.seeker.game.event.launched.LaunchedEvent;
import ru.homyakin.seeker.game.event.world_raid.entity.ActiveWorldRaid;
import ru.homyakin.seeker.game.group.entity.Group;

import java.util.List;

public interface SendWorldRaidBattleUpdateCommand {
    void sendUpdate(
        ActiveWorldRaid raid,
        LaunchedEvent event,
        int participantsCount,
        List<Group> groups,
        int requiredEnergy
    );
}
