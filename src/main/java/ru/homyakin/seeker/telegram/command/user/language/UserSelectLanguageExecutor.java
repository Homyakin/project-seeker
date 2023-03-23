package ru.homyakin.seeker.telegram.command.user.language;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;
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
        final var user = userService.changeLanguage(userService.getOrCreateFromPrivate(command.userId()), command.language());
        telegramSender.send(EditMessageTextBuilder.builder()
            .chatId(user.id())
            .messageId(command.messageId())
            .text(CommonLocalization.chooseLanguage(user.language()))
            .keyboard(InlineKeyboards.languageKeyboard(user.language()))
            .build()
        );
    }

}

