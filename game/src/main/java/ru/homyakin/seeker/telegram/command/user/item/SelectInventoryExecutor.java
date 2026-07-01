package ru.homyakin.seeker.telegram.command.user.item;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.item.ItemService;
import ru.homyakin.seeker.locale.item.ItemLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;

@Component
public class SelectInventoryExecutor extends CommandExecutor<SelectInventory> {
    private final UserService userService;
    private final TelegramSender telegramSender;
    private final ItemService itemService;

    public SelectInventoryExecutor(
        UserService userService,
        TelegramSender telegramSender,
        ItemService itemService
    ) {
        this.userService = userService;
        this.telegramSender = telegramSender;
        this.itemService = itemService;
    }

    @Override
    public void execute(SelectInventory command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        final var items = itemService.getPersonageItems(user.personageId());
        final var text = switch (command.section()) {
            case EQUIPMENT -> ItemLocalization.equipment(user.language(), items);
            case BAG -> ItemLocalization.bag(user.language(), items);
        };

        telegramSender.send(
            EditMessageTextBuilder.builder()
                .chatId(user.id())
                .messageId(command.messageId())
                .text(text)
                .keyboard(InlineKeyboards.inventoryKeyboard(user.language()))
                .build()
        );
    }
}
