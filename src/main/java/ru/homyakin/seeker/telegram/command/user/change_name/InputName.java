package ru.homyakin.seeker.telegram.command.user.change_name;

import org.telegram.telegrambots.meta.api.objects.Message;
import ru.homyakin.seeker.telegram.command.Command;

public record InputName(
    long userId,
    String name
) implements Command {
    public static InputName from(Message message) {
        return new InputName(
            message.getFrom().getId(),
            message.getText()
        );
    }
}
