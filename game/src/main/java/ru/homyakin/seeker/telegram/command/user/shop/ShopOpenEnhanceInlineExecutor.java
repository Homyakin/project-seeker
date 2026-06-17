package ru.homyakin.seeker.telegram.command.user.shop;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.shop.ShopInlineTgService;
import ru.homyakin.seeker.telegram.user.UserService;

@Component
public class ShopOpenEnhanceInlineExecutor extends CommandExecutor<ShopOpenEnhanceInline> {
    private final UserService userService;
    private final ShopInlineTgService shopInlineTgService;

    public ShopOpenEnhanceInlineExecutor(UserService userService, ShopInlineTgService shopInlineTgService) {
        this.userService = userService;
        this.shopInlineTgService = shopInlineTgService;
    }

    @Override
    public void execute(ShopOpenEnhanceInline command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        shopInlineTgService.showEnhanceTable(
            user.id(),
            user.language(),
            user.personageId(),
            command.messageId()
        );
    }
}
