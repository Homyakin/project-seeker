package ru.homyakin.seeker.telegram.command.user.feedback;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record InputFeedback(
    UserId userId,
    String theme,
    String feedback
) implements Command {
    public static InputFeedback from(Message message, String theme) {
        return new InputFeedback(UserId.from(message.getFrom().getId()), theme, message.getText());
    }
}
