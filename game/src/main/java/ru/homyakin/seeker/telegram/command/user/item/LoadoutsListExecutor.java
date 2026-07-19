package ru.homyakin.seeker.telegram.command.user.item;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;

@Component
public class LoadoutsListExecutor extends CommandExecutor<LoadoutsList> {
    private final UserService userService;
    private final LoadoutMessageService loadoutMessageService;

    public LoadoutsListExecutor(UserService userService, LoadoutMessageService loadoutMessageService) {
        this.userService = userService;
        this.loadoutMessageService = loadoutMessageService;
    }

    @Override
    public void execute(LoadoutsList command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        loadoutMessageService.editLoadoutsList(user, command.messageId());
    }
}
