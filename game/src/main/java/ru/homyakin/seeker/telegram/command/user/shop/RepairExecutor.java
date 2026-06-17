package ru.homyakin.seeker.telegram.command.user.shop;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.locale.shop.ShopLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class RepairExecutor extends CommandExecutor<Repair> {
    private final UserService userService;
    private final TelegramSender telegramSender;

    public RepairExecutor(UserService userService, TelegramSender telegramSender) {
        this.userService = userService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(Repair command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(user.id())
            .text(ShopLocalization.notBrokenItem(user.language()))
            .build()
        );
    }
}
