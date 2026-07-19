package ru.homyakin.seeker.telegram.command.user.shop;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.shop.ShopService;
import ru.homyakin.seeker.locale.shop.ShopLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;
import ru.homyakin.seeker.telegram.utils.ReplyKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class SellItemExecutor extends CommandExecutor<SellItem> {
    private final UserService userService;
    private final TelegramSender telegramSender;
    private final ShopService shopService;

    public SellItemExecutor(UserService userService, TelegramSender telegramSender, ShopService shopService) {
        this.userService = userService;
        this.telegramSender = telegramSender;
        this.shopService = shopService;
    }

    @Override
    public void execute(SellItem command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        final var item = shopService.getSellableItem(user.personageId(), command.itemId());
        if (item.isEmpty()) {
            telegramSender.send(SendMessageBuilder.builder()
                .chatId(user.id())
                .text(ShopLocalization.noItemAtPersonage(user.language()))
                .keyboard(ReplyKeyboards.mainKeyboard(user.language()))
                .build()
            );
            return;
        }

        final var loadoutNames = shopService.loadoutNamesForItem(user.personageId(), command.itemId());
        if (!loadoutNames.isEmpty()) {
            telegramSender.send(SendMessageBuilder.builder()
                .chatId(user.id())
                .text(ShopLocalization.confirmSellLoadout(
                    user.language(),
                    item.get(),
                    shopService.sellingPrice(item.get()),
                    loadoutNames
                ))
                .keyboard(InlineKeyboards.confirmSellItemKeyboard(user.language(), command.itemId()))
                .build()
            );
            return;
        }

        final var text = shopService.sellItem(user.personageId(), command.itemId())
            .fold(
                _ -> ShopLocalization.noItemAtPersonage(user.language()),
                sold -> ShopLocalization.successSell(user.language(), sold)
            );
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(user.id())
            .text(text)
            .keyboard(ReplyKeyboards.mainKeyboard(user.language()))
            .build()
        );
    }
}
