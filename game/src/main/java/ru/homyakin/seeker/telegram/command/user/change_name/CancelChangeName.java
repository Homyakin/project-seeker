package ru.homyakin.seeker.telegram.command.user.change_name;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.UserCommand;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record CancelChangeName(
    UserId userId
) implements UserCommand {
    public static CancelChangeName from(Message message) {
        return new CancelChangeName(
            UserId.from(message.getFrom().getId())
        );
    }
}
