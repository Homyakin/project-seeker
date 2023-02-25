package ru.homyakin.seeker.telegram.command.user.navigation;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.ReplyKeyboards;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;

@Component
public class BackExecutor extends CommandExecutor<Back> {
    private final UserService userService;
    private final TelegramSender telegramSender;

    public BackExecutor(UserService userService, TelegramSender telegramSender) {
        this.userService = userService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(Back command) {
        final var user = userService.getOrCreateFromPrivate(command.userId());
        telegramSender.send(
            TelegramMethods.createSendMessage(
                user.id(),
                CommonLocalization.mainMenu(user.language()),
                ReplyKeyboards.mainKeyboard(user.language())
            )
        );
    }
}
