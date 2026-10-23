package ru.homyakin.seeker.telegram.command.user.item;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.battle.PlayerCombatConfig;
import ru.homyakin.seeker.game.item.DefaultItemProvider;
import ru.homyakin.seeker.game.item.ItemService;
import ru.homyakin.seeker.game.personage.settings.action.GetPersonageSettingsCommand;
import ru.homyakin.seeker.locale.item.ItemLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;
import ru.homyakin.seeker.utils.PageUtils;

@Component
public class InventoryExecutor extends CommandExecutor<Inventory> {
    private final UserService userService;
    private final TelegramSender telegramSender;
    private final ItemService itemService;
    private final GetPersonageSettingsCommand getPersonageSettingsCommand;
    private final PlayerCombatConfig playerCombatConfig;
    private final DefaultItemProvider defaultItemProvider;

    public InventoryExecutor(
        UserService userService,
        TelegramSender telegramSender,
        ItemService itemService,
        GetPersonageSettingsCommand getPersonageSettingsCommand,
        PlayerCombatConfig playerCombatConfig,
        DefaultItemProvider defaultItemProvider
    ) {
        this.userService = userService;
        this.telegramSender = telegramSender;
        this.itemService = itemService;
        this.getPersonageSettingsCommand = getPersonageSettingsCommand;
        this.playerCombatConfig = playerCombatConfig;
        this.defaultItemProvider = defaultItemProvider;
    }

    @Override
    public void execute(Inventory command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        final var items = itemService.getPersonageItems(user.personageId());
        final var settings = getPersonageSettingsCommand.execute(user.personageId());
        final var defaults = defaultItemProvider.defaultsBySlot();
        final var builder = SendMessageBuilder.builder()
            .chatId(user.id());

        if (settings.compactItems()) {
            final var totalPages = ItemLocalization.bagTotalPages(items);
            final var page = PageUtils.clampPage(0, totalPages);
            builder
                .text(ItemLocalization.compactInventory(user.language(), items, page, defaults))
                .keyboard(InlineKeyboards.compactInventoryKeyboard(user.language(), page, totalPages));
        } else {
            builder
                .text(ItemLocalization.equipment(user.language(), items, playerCombatConfig.version(), defaults))
                .keyboard(InlineKeyboards.inventoryKeyboard(user.language()));
        }

        telegramSender.send(builder.build());
    }
}
