package ru.homyakin.seeker.telegram.command.user.item;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.item.ItemService;
import ru.homyakin.seeker.game.personage.settings.action.GetPersonageSettingsCommand;
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
    private final GetPersonageSettingsCommand getPersonageSettingsCommand;
    private final LoadoutMessageService loadoutMessageService;

    public SelectInventoryExecutor(
        UserService userService,
        TelegramSender telegramSender,
        ItemService itemService,
        GetPersonageSettingsCommand getPersonageSettingsCommand,
        LoadoutMessageService loadoutMessageService
    ) {
        this.userService = userService;
        this.telegramSender = telegramSender;
        this.itemService = itemService;
        this.getPersonageSettingsCommand = getPersonageSettingsCommand;
        this.loadoutMessageService = loadoutMessageService;
    }

    @Override
    public void execute(SelectInventory command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        if (command.section() == InventorySection.LOADOUTS) {
            loadoutMessageService.editLoadoutsList(user, command.messageId());
            return;
        }
        final var items = itemService.getPersonageItems(user.personageId());
        final var compactItems = getPersonageSettingsCommand.execute(user.personageId()).compactItems();
        if (compactItems) {
            telegramSender.send(
                EditMessageTextBuilder.builder()
                    .chatId(user.id())
                    .messageId(command.messageId())
                    .text(ItemLocalization.compactInventory(user.language(), items))
                    .keyboard(InlineKeyboards.compactInventoryKeyboard(user.language()))
                    .build()
            );
            return;
        }
        final var text = switch (command.section()) {
            case EQUIPMENT -> ItemLocalization.equipment(user.language(), items);
            case BAG -> ItemLocalization.bag(user.language(), items);
            case LOADOUTS -> throw new IllegalStateException("Handled above");
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
