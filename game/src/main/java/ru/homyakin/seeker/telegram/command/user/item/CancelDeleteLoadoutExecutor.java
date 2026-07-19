package ru.homyakin.seeker.telegram.command.user.item;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.item.loadout.action.EquipmentLoadoutService;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;

@Component
public class CancelDeleteLoadoutExecutor extends CommandExecutor<CancelDeleteLoadout> {
    private final UserService userService;
    private final EquipmentLoadoutService loadoutService;
    private final LoadoutMessageService loadoutMessageService;

    public CancelDeleteLoadoutExecutor(
        UserService userService,
        EquipmentLoadoutService loadoutService,
        LoadoutMessageService loadoutMessageService
    ) {
        this.userService = userService;
        this.loadoutService = loadoutService;
        this.loadoutMessageService = loadoutMessageService;
    }

    @Override
    public void execute(CancelDeleteLoadout command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        final var loadout = loadoutService.get(user.personageId(), command.loadoutId());
        if (loadout.isEmpty()) {
            loadoutMessageService.editLoadoutsList(user, command.messageId());
            return;
        }
        loadoutMessageService.editLoadoutDetail(user, command.messageId(), loadout.get());
    }
}
