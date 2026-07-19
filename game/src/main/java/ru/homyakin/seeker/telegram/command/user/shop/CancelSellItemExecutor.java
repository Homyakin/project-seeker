package ru.homyakin.seeker.telegram.command.user.shop;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.locale.shop.ShopLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;

@Component
public class CancelSellItemExecutor extends CommandExecutor<CancelSellItem> {
    private final UserService userService;
    private final TelegramSender telegramSender;

    public CancelSellItemExecutor(UserService userService, TelegramSender telegramSender) {
        this.userService = userService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(CancelSellItem command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        telegramSender.send(
            EditMessageTextBuilder.builder()
                .chatId(user.id())
                .messageId(command.messageId())
                .text(ShopLocalization.cancelSell(user.language()))
                .build()
        );
    }
}
