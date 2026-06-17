package ru.homyakin.seeker.telegram.command.user.shop;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.shop.ShopInlineTgService;
import ru.homyakin.seeker.telegram.user.UserService;

@Component
public class ShopSelectSlotExecutor extends CommandExecutor<ShopSelectSlot> {
    private final UserService userService;
    private final ShopInlineTgService shopInlineTgService;

    public ShopSelectSlotExecutor(UserService userService, ShopInlineTgService shopInlineTgService) {
        this.userService = userService;
        this.shopInlineTgService = shopInlineTgService;
    }

    @Override
    public void execute(ShopSelectSlot command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        shopInlineTgService.showSlotObjects(
            user.id(),
            user.language(),
            command.messageId(),
            command.slot()
        );
    }
}
