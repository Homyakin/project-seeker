package ru.homyakin.seeker.telegram.command.user.change_name;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record InputName(
    UserId userId,
    String name
) implements Command {
    public static InputName from(Message message) {
        return new InputName(
            UserId.from(message.getFrom().getId()),
            message.getText()
        );
    }
}
