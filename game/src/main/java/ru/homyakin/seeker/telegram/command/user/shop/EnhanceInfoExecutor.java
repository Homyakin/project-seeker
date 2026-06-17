package ru.homyakin.seeker.telegram.command.user.shop;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.shop.EnhanceService;
import ru.homyakin.seeker.locale.shop.ShopLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.ReplyKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class EnhanceInfoExecutor extends CommandExecutor<EnhanceInfo> {
    private final UserService userService;
    private final TelegramSender telegramSender;
    private final EnhanceService enhanceService;

    public EnhanceInfoExecutor(UserService userService, TelegramSender telegramSender, EnhanceService enhanceService) {
        this.userService = userService;
        this.telegramSender = telegramSender;
        this.enhanceService = enhanceService;
    }

    @Override
    public void execute(EnhanceInfo command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        final var text = enhanceService.availableAction(user.personageId(), command.itemId())
            .fold(
                _ -> ShopLocalization.noItemAtPersonage(user.language()),
                action -> ShopLocalization.enhanceItemInfo(user.language(), action)
            );
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(user.id())
            .text(text)
            .keyboard(ReplyKeyboards.shopKeyboard(user.language()))
            .build()
        );
    }
}
