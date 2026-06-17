package ru.homyakin.seeker.telegram.command.user.shop;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.shop.ShopInlineTgService;
import ru.homyakin.seeker.telegram.user.UserService;

@Component
public class ShopSelectRandomBoxesExecutor extends CommandExecutor<ShopSelectRandomBoxes> {
    private final UserService userService;
    private final ShopInlineTgService shopInlineTgService;

    public ShopSelectRandomBoxesExecutor(UserService userService, ShopInlineTgService shopInlineTgService) {
        this.userService = userService;
        this.shopInlineTgService = shopInlineTgService;
    }

    @Override
    public void execute(ShopSelectRandomBoxes command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        shopInlineTgService.showRandomBoxes(user.id(), user.language(), user.personageId(), command.messageId());
    }
}
