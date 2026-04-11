package ru.homyakin.seeker.telegram.command.online;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.online.LastOnlineUpdater;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.command.GroupCommand;
import ru.homyakin.seeker.telegram.command.UserCommand;
import ru.homyakin.seeker.telegram.command.UserGroupCommand;
import ru.homyakin.seeker.telegram.group.GroupTgService;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.utils.TimeUtils;

@Component
public class TelegramOnlineUpdater {
    private static final Logger logger = LoggerFactory.getLogger(TelegramOnlineUpdater.class);
    private final UserService userService;
    private final GroupTgService groupTgService;
    private final LastOnlineUpdater lastOnlineUpdater;

    public TelegramOnlineUpdater(
        UserService userService,
        GroupTgService groupTgService,
        LastOnlineUpdater lastOnlineUpdater
    ) {
        this.userService = userService;
        this.groupTgService = groupTgService;
        this.lastOnlineUpdater = lastOnlineUpdater;
    }

    public void update(Command command) {
        final var now = TimeUtils.moscowTime();
        try {
            if (command instanceof UserGroupCommand userGroupCommand) {
                var user = userService.forceGetFromGroup(userGroupCommand.userId());
                var group = groupTgService.getOrCreate(userGroupCommand.groupTgId());
                lastOnlineUpdater.touchPersonage(user.personageId(), now);
                lastOnlineUpdater.touchGroup(group.domainGroupId(), now);
                lastOnlineUpdater.touchActiveMembership(group.domainGroupId(), user.personageId(), now);
            } else if (command instanceof UserCommand userCommand) {
                var user = userService.forceGetFromGroup(userCommand.userId());
                lastOnlineUpdater.touchPersonage(user.personageId(), now);
            } else if (command instanceof GroupCommand groupCommand) {
                var group = groupTgService.getOrCreate(groupCommand.groupTgId());
                lastOnlineUpdater.touchGroup(group.domainGroupId(), now);
            }
        } catch (Exception e) {
            logger.warn("Failed to update last_online for {}", command, e);
        }
    }
}
