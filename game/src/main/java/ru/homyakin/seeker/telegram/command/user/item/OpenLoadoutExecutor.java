package ru.homyakin.seeker.telegram.command.user.item;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.item.loadout.action.EquipmentLoadoutService;
import ru.homyakin.seeker.locale.item.ItemLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class OpenLoadoutExecutor extends CommandExecutor<OpenLoadout> {
    private final UserService userService;
    private final EquipmentLoadoutService loadoutService;
    private final LoadoutMessageService loadoutMessageService;
    private final TelegramSender telegramSender;

    public OpenLoadoutExecutor(
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
    public void execute(OpenLoadout command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        final var loadout = loadoutService.get(user.personageId(), command.loadoutId());
        if (loadout.isEmpty()) {
            telegramSender.send(
                SendMessageBuilder.builder()
                    .chatId(user.id())
                    .text(ItemLocalization.loadoutNotFound(user.language()))
                    .build()
            );
            loadoutMessageService.editLoadoutsList(user, command.messageId());
            return;
        }
        loadoutMessageService.editLoadoutDetail(user, command.messageId(), loadout.get());
    }
}
