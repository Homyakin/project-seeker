package ru.homyakin.seeker.telegram.command.user.shop;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.shop.EnhanceService;
import ru.homyakin.seeker.game.shop.errors.RepairError;
import ru.homyakin.seeker.locale.shop.ShopLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class RepairExecutor extends CommandExecutor<Repair> {
    private final UserService userService;
    private final TelegramSender telegramSender;
    private final EnhanceService enhanceService;

    public RepairExecutor(UserService userService, TelegramSender telegramSender, EnhanceService enhanceService) {
        this.userService = userService;
        this.telegramSender = telegramSender;
        this.enhanceService = enhanceService;
    }

    @Override
    public void execute(Repair command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        final var text = enhanceService.repair(user.personageId(), command.itemId())
            .fold(
                error -> switch (error) {
                    case RepairError.NoSuchItem _ -> ShopLocalization.noItemAtPersonage(user.language());
                    case RepairError.NotBroken _ -> ShopLocalization.notBrokenItem(user.language());
                    case RepairError.NotEnoughMoney notEnoughMoney ->
                        ShopLocalization.notEnoughMoney(user.language(), notEnoughMoney.required());
                },
                action -> ShopLocalization.successRepair(user.language(), action)
            );
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(user.id())
            .text(text)
            .build()
        );
    }
}
