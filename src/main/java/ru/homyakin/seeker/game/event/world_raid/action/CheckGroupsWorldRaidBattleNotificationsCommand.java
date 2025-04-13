package ru.homyakin.seeker.game.event.world_raid.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.event.world_raid.entity.ActiveWorldRaidState;
import ru.homyakin.seeker.game.event.world_raid.entity.WorldRaidConfig;
import ru.homyakin.seeker.game.event.world_raid.entity.WorldRaidStorage;
import ru.homyakin.seeker.utils.TimeUtils;

@Component
public class CheckGroupsWorldRaidBattleNotificationsCommand {
    private static final Logger logger = LoggerFactory.getLogger(CheckGroupsWorldRaidBattleNotificationsCommand.class);
    private final WorldRaidConfig config;
    private final WorldRaidStorage storage;
    private final GetOrLaunchWorldRaidCommand getOrLaunchWorldRaidCommand;
    private final SendWorldRaidBattleToGroupCommand sendWorldRaidBattleToGroupCommand;

    public CheckGroupsWorldRaidBattleNotificationsCommand(
        WorldRaidConfig config,
        WorldRaidStorage storage,
        GetOrLaunchWorldRaidCommand getOrLaunchWorldRaidCommand,
        SendWorldRaidBattleToGroupCommand sendWorldRaidBattleToGroupCommand
    ) {
        this.config = config;
        this.storage = storage;
        this.getOrLaunchWorldRaidCommand = getOrLaunchWorldRaidCommand;
        this.sendWorldRaidBattleToGroupCommand = sendWorldRaidBattleToGroupCommand;
    }

    public void execute() {
        final var raid = getOrLaunchWorldRaidCommand.execute();
        if (!(raid.state() instanceof ActiveWorldRaidState.Battle)) {
            return;
        }
        final var groups = storage.getRegisteredGroupsToNotify(raid.id(), config.groupNotificationInterval());
        for (var group : groups) {
            logger.info("Sending notification about raid to group {}", group);
            sendWorldRaidBattleToGroupCommand.sendBattleToGroup(group);
            storage.updateGroupNotification(raid.id(), group, TimeUtils.moscowTime());
        }
    }
}
