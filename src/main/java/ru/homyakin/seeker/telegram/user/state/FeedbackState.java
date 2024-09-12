package ru.homyakin.seeker.telegram.user.state;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.command.type.FeedbackCommandType;
import ru.homyakin.seeker.telegram.command.user.feedback.CancelFeedback;
import ru.homyakin.seeker.telegram.command.user.feedback.ChooseFeedbackTheme;
import ru.homyakin.seeker.telegram.command.user.feedback.FeedbackTheme;
import ru.homyakin.seeker.telegram.command.user.feedback.InitFeedback;
import ru.homyakin.seeker.telegram.command.user.feedback.InputFeedback;
import ru.homyakin.seeker.telegram.command.user.feedback.InvalidTheme;

public sealed interface FeedbackState extends UserState {

    record ChooseFeedbackThemeState() implements FeedbackState {
        @Override
        public Command nextCommand(Message message) {
            return FeedbackCommandType.getFromString(message.getText())
                .map(command -> switch (command) {
                    case SUGGEST_TEXT -> ChooseFeedbackTheme.from(message, FeedbackTheme.SUGGEST_TEXT);
                    case OTHER_THEME -> ChooseFeedbackTheme.from(message, FeedbackTheme.OTHER_THEME);
                    case BACK -> CancelFeedback.from(message);
                })
                .orElseGet(() -> InvalidTheme.from(message));
        }
    }

    record InputFeedbackState(String theme) implements FeedbackState {
        @Override
        public Command nextCommand(Message message) {
            return FeedbackCommandType.getFromString(message.getText())
                .<Command>map(command -> switch (command) {
                    case BACK -> InitFeedback.from(message);
                    default -> null;
                })
                .orElseGet(() -> InputFeedback.from(message, theme));
        }
    }
}
