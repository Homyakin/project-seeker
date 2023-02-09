package ru.homyakin.seeker.telegram.command.user;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.utils.ReplyKeyboards;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;
import ru.homyakin.seeker.telegram.user.UserService;

@Component
public class StartUserExecutor extends CommandExecutor<StartUser> {
    private final UserService userService;
    private final TelegramSender telegramSender;

    public StartUserExecutor(UserService userService, TelegramSender telegramSender) {
        this.userService = userService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(StartUser command) {
        final var user = userService.getOrCreateFromPrivate(command.userId());
        telegramSender.send(
            TelegramMethods.createSendMessage(
                user.id(),
                CommonLocalization.welcomeUser(user.language()),
                ReplyKeyboards.mainKeyboard(user.language())
            )
        );
    }
}
