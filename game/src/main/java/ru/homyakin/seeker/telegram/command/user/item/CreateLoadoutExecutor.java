package ru.homyakin.seeker.telegram.command.user.item;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.item.loadout.action.EquipmentLoadoutService;
import ru.homyakin.seeker.locale.item.ItemLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.user.state.LoadoutNameState;
import ru.homyakin.seeker.telegram.user.state.UserStateService;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;

@Component
public class CreateLoadoutExecutor extends CommandExecutor<CreateLoadout> {
    private final UserService userService;
    private final EquipmentLoadoutService loadoutService;
    private final UserStateService userStateService;
    private final LoadoutMessageService loadoutMessageService;
    private final TelegramSender telegramSender;

    public CreateLoadoutExecutor(
        UserService userService,
        EquipmentLoadoutService loadoutService,
        UserStateService userStateService,
        LoadoutMessageService loadoutMessageService,
        TelegramSender telegramSender
    ) {
        this.userService = userService;
        this.loadoutService = loadoutService;
        this.userStateService = userStateService;
        this.loadoutMessageService = loadoutMessageService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(CreateLoadout command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        if (!loadoutService.canCreate(user.personageId())) {
            telegramSender.send(TelegramMethods.createAnswerCallbackQuery(
                command.callbackId(),
                ItemLocalization.maxLoadoutsReached(user.language())
            ));
            return;
        }
        userStateService.setUserState(user, new LoadoutNameState.CreateLoadout(command.messageId()));
        loadoutMessageService.editCreateLoadoutPrompt(user, command.messageId());
    }
}
