package ru.homyakin.seeker.telegram.command.user.item;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.item.loadout.action.EquipmentLoadoutService;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;

@Component
public class ToggleDefaultLoadoutExecutor extends CommandExecutor<ToggleDefaultLoadout> {
    private final UserService userService;
    private final EquipmentLoadoutService loadoutService;
    private final LoadoutMessageService loadoutMessageService;

    public ToggleDefaultLoadoutExecutor(
        UserService userService,
        EquipmentLoadoutService loadoutService,
        LoadoutMessageService loadoutMessageService
    ) {
        this.userService = userService;
        this.loadoutService = loadoutService;
        this.loadoutMessageService = loadoutMessageService;
    }

    @Override
    public void execute(ToggleDefaultLoadout command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        if (loadoutService.get(user.personageId(), command.loadoutId()).isEmpty()) {
            loadoutMessageService.editDefaultLoadoutsMenu(user, command.messageId());
            return;
        }
        loadoutService.toggleDefault(user.personageId(), command.loadoutId(), command.eventType());
        loadoutMessageService.editDefaultLoadoutForEvent(user, command.messageId(), command.eventType());
    }
}
