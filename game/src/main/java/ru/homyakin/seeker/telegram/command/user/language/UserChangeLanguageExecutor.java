package ru.homyakin.seeker.telegram.command.user.language;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;
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
        final var user = userService.forceGetFromPrivate(command.userId());
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(command.userId())
            .text(CommonLocalization.chooseLanguage(user.language()))
            .keyboard(InlineKeyboards.languageKeyboard(user.language()))
            .build()
        );
    }
}
