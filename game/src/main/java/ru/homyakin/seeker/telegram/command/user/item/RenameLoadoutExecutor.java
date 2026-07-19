package ru.homyakin.seeker.telegram.command.user.item;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.item.loadout.action.EquipmentLoadoutService;
import ru.homyakin.seeker.locale.item.ItemLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.user.state.LoadoutNameState;
import ru.homyakin.seeker.telegram.user.state.UserStateService;
import ru.homyakin.seeker.telegram.utils.ReplyKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class RenameLoadoutExecutor extends CommandExecutor<RenameLoadout> {
    private final UserService userService;
    private final EquipmentLoadoutService loadoutService;
    private final UserStateService userStateService;
    private final LoadoutMessageService loadoutMessageService;
    private final TelegramSender telegramSender;

    public RenameLoadoutExecutor(
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
    public void execute(RenameLoadout command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        final var loadout = loadoutService.get(user.personageId(), command.loadoutId());
        if (loadout.isEmpty()) {
            telegramSender.send(SendMessageBuilder.builder()
                .chatId(user.id())
                .text(ItemLocalization.loadoutNotFound(user.language()))
                .build()
            );
            loadoutMessageService.editLoadoutsList(user, command.messageId());
            return;
        }
        userStateService.setUserState(
            user,
            new LoadoutNameState.RenameLoadout(command.loadoutId(), command.messageId())
        );
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(user.id())
            .text(ItemLocalization.initRenameLoadout(user.language(), loadout.get().name()))
            .keyboard(ReplyKeyboards.loadoutNameKeyboard(user.language()))
            .build()
        );
    }
}
