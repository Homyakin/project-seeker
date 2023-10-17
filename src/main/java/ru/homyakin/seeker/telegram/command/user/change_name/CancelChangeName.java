package ru.homyakin.seeker.telegram.command.user.change_name;

import org.telegram.telegrambots.meta.api.objects.Message;
import ru.homyakin.seeker.telegram.command.Command;

public record CancelChangeName(
    long userId
) implements Command {
    public static CancelChangeName from(Message message) {
        return new CancelChangeName(
            message.getFrom().getId()
        );
    }
}
