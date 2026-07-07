package ru.homyakin.seeker.telegram.command.user.item;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.item.ItemService;
import ru.homyakin.seeker.game.personage.settings.action.GetPersonageSettingsCommand;
import ru.homyakin.seeker.locale.item.ItemLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class InventoryExecutor extends CommandExecutor<Inventory> {
    private final UserService userService;
    private final TelegramSender telegramSender;
    private final ItemService itemService;
    private final GetPersonageSettingsCommand getPersonageSettingsCommand;

    public InventoryExecutor(
        UserService userService,
        TelegramSender telegramSender,
        ItemService itemService,
        GetPersonageSettingsCommand getPersonageSettingsCommand
    ) {
        this.userService = userService;
        this.telegramSender = telegramSender;
        this.itemService = itemService;
        this.getPersonageSettingsCommand = getPersonageSettingsCommand;
    }

    @Override
    public void execute(Inventory command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        final var items = itemService.getPersonageItems(user.personageId());
        final var settings = getPersonageSettingsCommand.execute(user.personageId());
        final var builder = SendMessageBuilder.builder()
            .chatId(user.id());

        if (settings.compactItems()) {
            builder.text(ItemLocalization.compactInventory(user.language(), items));
        } else {
            builder
                .text(ItemLocalization.equipment(user.language(), items))
                .keyboard(InlineKeyboards.inventoryKeyboard(user.language()));
        }

        telegramSender.send(builder.build());
    }
}
