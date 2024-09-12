package ru.homyakin.seeker.telegram.command.user.feedback;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.locale.feedback.FeedbackLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.ReplyKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class InvalidThemeExecutor extends CommandExecutor<InvalidTheme> {
    private final UserService userService;
    private final TelegramSender telegramSender;

    public InvalidThemeExecutor(
        UserService userService,
        TelegramSender telegramSender
    ) {
        this.userService = userService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(InvalidTheme command) {
        final var user = userService.getOrCreateFromPrivate(command.userId());
        telegramSender.send(
            SendMessageBuilder
                .builder()
                .chatId(user.id())
                .keyboard(ReplyKeyboards.chooseFeedbackThemeKeyboard(user.language()))
                .text(FeedbackLocalization.invalidTheme(user.language()))
                .build()
        );
    }
}
