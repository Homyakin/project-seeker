package ru.homyakin.seeker.telegram.command.user.feedback;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.locale.feedback.FeedbackLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.user.state.FeedbackConfig;
import ru.homyakin.seeker.telegram.user.state.UserStateService;
import ru.homyakin.seeker.telegram.utils.ReplyKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class InputFeedbackExecutor extends CommandExecutor<InputFeedback> {
    private final UserService userService;
    private final UserStateService userStateService;
    private final PersonageService personageService;
    private final TelegramSender telegramSender;
    private final FeedbackConfig config;

    public InputFeedbackExecutor(
        UserService userService,
        UserStateService userStateService,
        PersonageService personageService,
        TelegramSender telegramSender,
        FeedbackConfig config
    ) {
        this.userService = userService;
        this.userStateService = userStateService;
        this.personageService = personageService;
        this.telegramSender = telegramSender;
        this.config = config;
    }

    @Override
    public void execute(InputFeedback command) {
        final var user = userService.getOrCreateFromPrivate(command.userId());
        final var personage = personageService.getByIdForce(user.personageId());
        final var result = telegramSender.send(
            SendMessageBuilder
                .builder()
                .chatId(config.adminGroup())
                .text(FeedbackLocalization.feedback(user.language(), personage, command))
                .build()
        );
        final String text;
        if (result.isRight()) {
            text = FeedbackLocalization.feedbackSent(user.language());
        } else {
            text = FeedbackLocalization.feedbackErrorSent(user.language());
        }
        telegramSender.send(
            SendMessageBuilder
                .builder()
                .chatId(command.userId())
                .text(text)
                .keyboard(ReplyKeyboards.mainKeyboard(user.language()))
                .build()
        );

        userStateService.clearUserState(user);
    }
}
