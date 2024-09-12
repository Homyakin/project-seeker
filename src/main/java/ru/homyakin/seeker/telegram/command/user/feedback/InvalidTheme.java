package ru.homyakin.seeker.telegram.command.user.feedback;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record InvalidTheme(
    UserId userId
) implements Command {
    public static InvalidTheme from(Message message) {
        return new InvalidTheme(UserId.from(message.getFrom().getId()));
    }
}
