package ru.homyakin.seeker.telegram.command.user.shop;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.shop.ShopService;
import ru.homyakin.seeker.game.shop.errors.BuyItemError;
import ru.homyakin.seeker.locale.shop.ShopLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.ReplyKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class BuyItemExecutor extends CommandExecutor<BuyItem> {
    private final UserService userService;
    private final TelegramSender telegramSender;
    private final ShopService shopService;

    public BuyItemExecutor(UserService userService, TelegramSender telegramSender, ShopService shopService) {
        this.userService = userService;
        this.telegramSender = telegramSender;
        this.shopService = shopService;
    }

    @Override
    public void execute(BuyItem command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        if (command.type().isEmpty()) {
            telegramSender.send(SendMessageBuilder.builder()
                .chatId(user.id())
                .text(ShopLocalization.incorrectBuyingItem(user.language()))
                .keyboard(ReplyKeyboards.mainKeyboard(user.language()))
                .build()
            );
            return;
        }
        final var text = shopService.buyItem(user.personageId(), command.type().get())
            .fold(
                error -> switch (error) {
                    case BuyItemError.NotEnoughSpaceInBag _ -> ShopLocalization.notEnoughSpaceInBag(user.language());
                    case BuyItemError.NotEnoughMoney _ -> ShopLocalization.notEnoughMoney(user.language());
                },
                item -> ShopLocalization.successBuy(user.language(), item)
            );
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(user.id())
            .text(text)
            .keyboard(ReplyKeyboards.mainKeyboard(user.language()))
            .build()
        );
    }
}
