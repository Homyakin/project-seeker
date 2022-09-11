package ru.homyakin.seeker.command.user.language;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.command.CommandExecutor;
import ru.homyakin.seeker.command.CommandText;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Localization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.utils.Keyboards;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;
import ru.homyakin.seeker.user.UserService;

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
        final var user = userService.getOrCreate(command.userId(), true);
        final var language = Language.getOrDefault(Integer.valueOf(command.data().split(CommandText.CALLBACK_DELIMITER)[1]));
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

