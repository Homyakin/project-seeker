package ru.homyakin.seeker.telegram.command.user.shop;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.item.ItemService;
import ru.homyakin.seeker.locale.shop.ShopLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.command.type.CommandType;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;
import ru.homyakin.seeker.telegram.utils.ShopKeyboards;
import ru.homyakin.seeker.utils.PageUtils;

@Component
public class OpenEnhanceTableExecutor extends CommandExecutor<OpenEnhanceTable> {
    private final UserService userService;
    private final TelegramSender telegramSender;
    private final ItemService itemService;

    public OpenEnhanceTableExecutor(UserService userService, TelegramSender telegramSender, ItemService itemService) {
        this.userService = userService;
        this.telegramSender = telegramSender;
        this.itemService = itemService;
    }

    @Override
    public void execute(OpenEnhanceTable command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        final var inventory = itemService.getPersonageItems(user.personageId());
        final var totalPages = ShopLocalization.enhanceBagTotalPages(inventory);
        final var page = PageUtils.clampPage(0, totalPages);
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(user.id())
            .text(ShopLocalization.enhanceTable(user.language(), inventory, page))
            .keyboard(ShopKeyboards.navigationKeyboard(
                user.language(),
                CommandType.SHOP_ENHANCE_INLINE,
                page,
                totalPages
            ))
            .build()
        );
    }
}
