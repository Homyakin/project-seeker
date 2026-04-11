package ru.homyakin.seeker.telegram.command.user.change_name;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.UserCommand;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record RepeatChangeName(
    UserId userId
) implements UserCommand {
    public static RepeatChangeName from(Message message) {
        return new RepeatChangeName(UserId.from(message.getFrom().getId()));
    }
}
