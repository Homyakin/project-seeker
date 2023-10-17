package ru.homyakin.seeker.telegram.command.user.change_name;

import org.telegram.telegrambots.meta.api.objects.Message;
import ru.homyakin.seeker.telegram.command.Command;

public record ConfirmChangeName(
    long userId,
    String name
) implements Command {
    public static ConfirmChangeName from(Message message, String name) {
        return new ConfirmChangeName(message.getFrom().getId(), name);
    }
}
