package ru.homyakin.seeker.telegram.command.user.change_name;

import org.telegram.telegrambots.meta.api.objects.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record CancelChangeName(
    UserId userId
) implements Command {
    public static CancelChangeName from(Message message) {
        return new CancelChangeName(
            UserId.from(message.getFrom().getId())
        );
    }
}
