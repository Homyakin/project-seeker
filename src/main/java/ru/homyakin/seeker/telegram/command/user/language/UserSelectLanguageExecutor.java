package ru.homyakin.seeker.telegram.command.user.language;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Localization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.utils.Keyboards;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;
import ru.homyakin.seeker.telegram.user.UserService;

@Component
public class UserSelectLanguageExecutor extends CommandExecutor<UserSelectLanguage> {
    private final UserService userService;
    private final TelegramSender telegramSender;

    public UserSelectLanguageExecutor(
        UserService userService,
        TelegramSender telegramSender
    ) {
        this.userService = userService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(UserSelectLanguage command) {
        final var user = userService.getOrCreateFromPrivate(command.userId());
        final var language = Language.getOrDefault(command.getLanguageId());
        final var updatedUser = userService.changeLanguage(user, language);
        telegramSender.send(
            TelegramMethods.createEditMessageText(
                command.userId(),
                command.messageId(),
                Localization.get(updatedUser.language()).chooseLanguage(),
                Keyboards.languageKeyboard(updatedUser.language())
            )
        );
    }

}

