package ru.homyakin.seeker.telegram.command.user.shop;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.contraband.action.ContrabandService;
import ru.homyakin.seeker.game.personage.settings.action.GetPersonageSettingsCommand;
import ru.homyakin.seeker.game.shop.ShopService;
import ru.homyakin.seeker.locale.shop.ShopLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.command.type.CommandType;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;
import ru.homyakin.seeker.telegram.utils.ShopKeyboards;
import ru.homyakin.seeker.utils.PageUtils;

@Component
public class OpenShopExecutor extends CommandExecutor<OpenShop> {
    private final UserService userService;
    private final TelegramSender telegramSender;
    private final ShopService shopService;
    private final ContrabandService contrabandService;
    private final GetPersonageSettingsCommand getPersonageSettingsCommand;

    public OpenShopExecutor(
        UserService userService,
        TelegramSender telegramSender,
        ShopService shopService,
        ContrabandService contrabandService,
        GetPersonageSettingsCommand getPersonageSettingsCommand
    ) {
        this.userService = userService;
        this.telegramSender = telegramSender;
        this.shopService = shopService;
        this.contrabandService = contrabandService;
        this.getPersonageSettingsCommand = getPersonageSettingsCommand;
    }

    @Override
    public void execute(OpenShop command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        final var activeContraband = contrabandService.getActiveContraband(user.personageId());
        final var compactItems = getPersonageSettingsCommand.execute(user.personageId()).compactItems();
        final var items = shopService.getShopItems(user.personageId());
        final var totalPages = ShopLocalization.sellingTotalPages(items);
        final var page = PageUtils.clampPage(0, totalPages);
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(user.id())
            .text(ShopLocalization.menu(
                user.language(),
                items,
                activeContraband,
                compactItems,
                page
            ))
            .keyboard(ShopKeyboards.navigationKeyboard(
                user.language(),
                CommandType.SHOP_RANDOM_BOXES,
                page,
                totalPages
            ))
            .build()
        );
    }
}
