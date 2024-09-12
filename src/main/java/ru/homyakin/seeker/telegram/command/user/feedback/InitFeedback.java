package ru.homyakin.seeker.telegram.command.user.feedback;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record InitFeedback(
    UserId userId
) implements Command {
    public static InitFeedback from(Message message) {
        return new InitFeedback(UserId.from(message.getFrom().getId()));
    }
}
