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
import ru.homyakin.seeker.utils.PageUtils;

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
            final var totalPages = ItemLocalization.bagTotalPages(items);
            final var page = PageUtils.clampPage(command.page(), totalPages);
            telegramSender.send(
                EditMessageTextBuilder.builder()
                    .chatId(user.id())
                    .messageId(command.messageId())
                    .text(ItemLocalization.compactInventory(user.language(), items, page))
                    .keyboard(InlineKeyboards.compactInventoryKeyboard(user.language(), page, totalPages))
                    .build()
            );
            return;
        }
        if (command.section() == InventorySection.BAG) {
            final var totalPages = ItemLocalization.bagTotalPages(items);
            final var page = PageUtils.clampPage(command.page(), totalPages);
            telegramSender.send(
                EditMessageTextBuilder.builder()
                    .chatId(user.id())
                    .messageId(command.messageId())
                    .text(ItemLocalization.bag(user.language(), items, page))
                    .keyboard(InlineKeyboards.inventoryKeyboard(user.language(), page, totalPages))
                    .build()
            );
            return;
        }

        telegramSender.send(
            EditMessageTextBuilder.builder()
                .chatId(user.id())
                .messageId(command.messageId())
                .text(ItemLocalization.equipment(user.language(), items))
                .keyboard(InlineKeyboards.inventoryKeyboard(user.language()))
                .build()
        );
    }
}
