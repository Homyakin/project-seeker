package ru.homyakin.seeker.telegram.command.user.feedback;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.locale.feedback.FeedbackLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.user.state.FeedbackState;
import ru.homyakin.seeker.telegram.user.state.UserStateService;
import ru.homyakin.seeker.telegram.utils.ReplyKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class ChooseFeedbackThemeExecutor extends CommandExecutor<ChooseFeedbackTheme> {
    private final UserService userService;
    private final UserStateService userStateService;
    private final TelegramSender telegramSender;

    public ChooseFeedbackThemeExecutor(
        UserService userService,
        UserStateService userStateService,
        TelegramSender telegramSender
    ) {
        this.userService = userService;
        this.userStateService = userStateService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(ChooseFeedbackTheme command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        userStateService.setUserState(user, new FeedbackState.InputFeedbackState(command.themeName()));
        final var text = switch (command.theme()) {
            case SUGGEST_TEXT -> FeedbackLocalization.inputTextSuggestion(user.language());
            case OTHER_THEME -> FeedbackLocalization.inputFeedback(user.language());
        };
        telegramSender.send(
            SendMessageBuilder
                .builder()
                .chatId(user.id())
                .keyboard(ReplyKeyboards.inputFeedbackKeyboard(user.language()))
                .text(text)
                .build()
        );
    }
}
