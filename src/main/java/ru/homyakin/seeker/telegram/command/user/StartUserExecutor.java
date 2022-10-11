package ru.homyakin.seeker.telegram.command.user;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.locale.Localization;
import ru.homyakin.seeker.telegram.TelegramSender;
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
        final var chat = userService.getOrCreate(command.userId(), true);
        telegramSender.send(
            TelegramMethods.createSendMessage(
                chat.id(),
                Localization.get(chat.language()).welcomeUser()
            )
        );
    }
}
