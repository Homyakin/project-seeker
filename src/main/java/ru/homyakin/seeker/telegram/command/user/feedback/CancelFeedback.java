package ru.homyakin.seeker.telegram.command.user.feedback;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record CancelFeedback(
    UserId userId
) implements Command {
    public static CancelFeedback from(Message message) {
        return new CancelFeedback(UserId.from(message.getFrom().getId()));
    }
}
