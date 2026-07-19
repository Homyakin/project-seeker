package ru.homyakin.seeker.telegram.command.user.item;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.item.loadout.action.EquipmentLoadoutService;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;

@Component
public class OpenDefaultLoadoutEventExecutor extends CommandExecutor<OpenDefaultLoadoutEvent> {
    private final UserService userService;
    private final LoadoutMessageService loadoutMessageService;

    public OpenDefaultLoadoutEventExecutor(
        UserService userService,
        LoadoutMessageService loadoutMessageService
    ) {
        this.userService = userService;
        this.loadoutMessageService = loadoutMessageService;
    }

    @Override
    public void execute(OpenDefaultLoadoutEvent command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        if (!EquipmentLoadoutService.DEFAULT_LOADOUT_EVENT_TYPES.contains(command.eventType())) {
            loadoutMessageService.editDefaultLoadoutsMenu(user, command.messageId());
            return;
        }
        loadoutMessageService.editDefaultLoadoutForEvent(user, command.messageId(), command.eventType());
    }
}
