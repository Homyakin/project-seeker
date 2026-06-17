package ru.homyakin.seeker.telegram.command.user.feedback;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.UserCommand;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record InvalidTheme(
    UserId userId
) implements UserCommand {
    public static InvalidTheme from(Message message) {
        return new InvalidTheme(UserId.from(message.getFrom().getId()));
    }
}
