package ru.homyakin.seeker.telegram.command.user.shop;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.item.ItemService;
import ru.homyakin.seeker.locale.shop.ShopLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.ReplyKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

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
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(user.id())
            .text(ShopLocalization.enhanceTable(user.language(), itemService.getPersonageItems(user.personageId())))
            .keyboard(ReplyKeyboards.shopKeyboard(user.language()))
            .build()
        );
    }
}
