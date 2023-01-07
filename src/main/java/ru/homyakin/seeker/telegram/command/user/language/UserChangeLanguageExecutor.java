package ru.homyakin.seeker.telegram.command.user.language;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.locale.Localization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.utils.Keyboards;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;
import ru.homyakin.seeker.telegram.user.UserService;

@Component
public class UserChangeLanguageExecutor extends CommandExecutor<UserChangeLanguage> {
    private final UserService userService;
    private final TelegramSender telegramSender;

    public UserChangeLanguageExecutor(
        UserService userService,
        TelegramSender telegramSender
    ) {
        this.userService = userService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(UserChangeLanguage command) {
        final var user = userService.getOrCreateFromPrivate(command.userId());
        telegramSender.send(
            TelegramMethods.createSendMessage(
                command.userId(),
                Localization.get(user.language()).chooseLanguage(),
                Keyboards.languageKeyboard(user.language())
            )
        );
    }
}
