package ru.homyakin.seeker.telegram.command.user.shop;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.contraband.action.ContrabandService;
import ru.homyakin.seeker.game.shop.ShopService;
import ru.homyakin.seeker.locale.shop.ShopLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.ReplyKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class OpenShopExecutor extends CommandExecutor<OpenShop> {
    private final UserService userService;
    private final TelegramSender telegramSender;
    private final ShopService shopService;
    private final ContrabandService contrabandService;

    public OpenShopExecutor(
        UserService userService,
        TelegramSender telegramSender,
        ShopService shopService,
        ContrabandService contrabandService
    ) {
        this.userService = userService;
        this.telegramSender = telegramSender;
        this.shopService = shopService;
        this.contrabandService = contrabandService;
    }

    @Override
    public void execute(OpenShop command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        final var activeContraband = contrabandService.getActiveContraband(user.personageId());
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(user.id())
            .text(ShopLocalization.menu(user.language(), shopService.getShopItems(user.personageId()), activeContraband))
            .keyboard(ReplyKeyboards.shopKeyboard(user.language()))
            .build()
        );
    }
}
