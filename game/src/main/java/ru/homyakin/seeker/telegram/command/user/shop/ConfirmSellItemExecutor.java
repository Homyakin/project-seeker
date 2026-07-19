package ru.homyakin.seeker.telegram.command.user.shop;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.shop.ShopService;
import ru.homyakin.seeker.locale.shop.ShopLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;

@Component
public class ConfirmSellItemExecutor extends CommandExecutor<ConfirmSellItem> {
    private final UserService userService;
    private final TelegramSender telegramSender;
    private final ShopService shopService;

    public ConfirmSellItemExecutor(
        UserService userService,
        TelegramSender telegramSender,
        ShopService shopService
    ) {
        this.userService = userService;
        this.telegramSender = telegramSender;
        this.shopService = shopService;
    }

    @Override
    public void execute(ConfirmSellItem command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        final var text = shopService.sellItem(user.personageId(), command.itemId())
            .fold(
                _ -> ShopLocalization.noItemAtPersonage(user.language()),
                sold -> ShopLocalization.successSell(user.language(), sold)
            );
        telegramSender.send(
            EditMessageTextBuilder.builder()
                .chatId(user.id())
                .messageId(command.messageId())
                .text(text)
                .build()
        );
    }
}
