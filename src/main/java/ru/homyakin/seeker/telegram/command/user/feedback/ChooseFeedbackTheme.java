package ru.homyakin.seeker.telegram.command.user.feedback;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record ChooseFeedbackTheme(
    UserId userId,
    FeedbackTheme theme,
    String themeName
) implements Command {
    public static ChooseFeedbackTheme from(Message message, FeedbackTheme theme) {
        return new ChooseFeedbackTheme(
            UserId.from(message.getFrom().getId()),
            theme,
            message.getText()
        );
    }
}
