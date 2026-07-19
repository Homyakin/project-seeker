package ru.homyakin.seeker.telegram.command.user.item;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.item.loadout.action.EquipmentLoadoutService;
import ru.homyakin.seeker.locale.item.ItemLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;

@Component
public class DeleteLoadoutExecutor extends CommandExecutor<DeleteLoadout> {
    private final UserService userService;
    private final EquipmentLoadoutService loadoutService;
    private final LoadoutMessageService loadoutMessageService;
    private final TelegramSender telegramSender;

    public DeleteLoadoutExecutor(
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
    public void execute(DeleteLoadout command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        final var existing = loadoutService.get(user.personageId(), command.loadoutId());
        if (existing.isEmpty()) {
            telegramSender.send(TelegramMethods.createAnswerCallbackQuery(
                command.callbackId(),
                ItemLocalization.loadoutNotFound(user.language())
            ));
            loadoutMessageService.editLoadoutsList(user, command.messageId());
            return;
        }
        telegramSender.send(
            EditMessageTextBuilder.builder()
                .chatId(user.id())
                .messageId(command.messageId())
                .text(ItemLocalization.confirmDeleteLoadout(user.language(), existing.get().name()))
                .keyboard(InlineKeyboards.confirmDeleteLoadoutKeyboard(user.language(), command.loadoutId()))
                .build()
        );
    }
}
