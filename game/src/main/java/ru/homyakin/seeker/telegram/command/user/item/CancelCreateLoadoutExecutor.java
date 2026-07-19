package ru.homyakin.seeker.telegram.command.user.item;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.user.state.UserStateService;

@Component
public class CancelCreateLoadoutExecutor extends CommandExecutor<CancelCreateLoadout> {
    private final UserService userService;
    private final UserStateService userStateService;
    private final LoadoutMessageService loadoutMessageService;

    public CancelCreateLoadoutExecutor(
        UserService userService,
        UserStateService userStateService,
        LoadoutMessageService loadoutMessageService
    ) {
        this.userService = userService;
        this.userStateService = userStateService;
        this.loadoutMessageService = loadoutMessageService;
    }

    @Override
    public void execute(CancelCreateLoadout command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        userStateService.clearUserState(user);
        loadoutMessageService.editLoadoutsList(user, command.messageId());
    }
}
