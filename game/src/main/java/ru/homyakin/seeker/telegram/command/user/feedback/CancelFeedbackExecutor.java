package ru.homyakin.seeker.telegram.command.user.feedback;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.locale.feedback.FeedbackLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.user.state.UserStateService;
import ru.homyakin.seeker.telegram.utils.ReplyKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class CancelFeedbackExecutor extends CommandExecutor<CancelFeedback> {
    private final UserService userService;
    private final UserStateService userStateService;
    private final TelegramSender telegramSender;

    public CancelFeedbackExecutor(
        UserService userService,
        UserStateService userStateService,
        TelegramSender telegramSender
    ) {
        this.userService = userService;
        this.userStateService = userStateService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(CancelFeedback command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        userStateService.clearUserState(user);
        telegramSender.send(
            SendMessageBuilder
                .builder()
                .chatId(user.id())
                .keyboard(ReplyKeyboards.mainKeyboard(user.language()))
                .text(FeedbackLocalization.cancelFeedback(user.language()))
                .build()
        );
    }
}
