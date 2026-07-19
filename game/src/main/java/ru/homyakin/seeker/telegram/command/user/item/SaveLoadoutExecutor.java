package ru.homyakin.seeker.telegram.command.user.item;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.item.loadout.action.EquipmentLoadoutService;
import ru.homyakin.seeker.locale.item.ItemLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;

@Component
public class SaveLoadoutExecutor extends CommandExecutor<SaveLoadout> {
    private final UserService userService;
    private final EquipmentLoadoutService loadoutService;
    private final LoadoutMessageService loadoutMessageService;
    private final TelegramSender telegramSender;

    public SaveLoadoutExecutor(
        UserService userService,
        EquipmentLoadoutService loadoutService,
        LoadoutMessageService loadoutMessageService,
        TelegramSender telegramSender
    ) {
        this.userService = userService;
        this.loadoutService = loadoutService;
        this.loadoutMessageService = loadoutMessageService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(SaveLoadout command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        final var result = loadoutService.saveCurrent(user.personageId(), command.loadoutId());
        if (result.isLeft()) {
            telegramSender.send(TelegramMethods.createAnswerCallbackQuery(
                command.callbackId(),
                ItemLocalization.loadoutNotFound(user.language())
            ));
            loadoutMessageService.editLoadoutsList(user, command.messageId());
            return;
        }
        final var loadout = result.get();
        telegramSender.send(TelegramMethods.createAnswerCallbackQuery(
            command.callbackId(),
            ItemLocalization.successSaveLoadout(user.language(), loadout.name())
        ));
        loadoutMessageService.editLoadoutDetail(user, command.messageId(), loadout);
    }
}
