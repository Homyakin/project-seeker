package ru.homyakin.seeker.telegram.command.user.item;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;

@Component
public class DefaultLoadoutsExecutor extends CommandExecutor<DefaultLoadouts> {
    private final UserService userService;
    private final LoadoutMessageService loadoutMessageService;

    public DefaultLoadoutsExecutor(UserService userService, LoadoutMessageService loadoutMessageService) {
        this.userService = userService;
        this.loadoutMessageService = loadoutMessageService;
    }

    @Override
    public void execute(DefaultLoadouts command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        loadoutMessageService.editDefaultLoadoutsMenu(user, command.messageId());
    }
}
