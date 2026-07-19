package ru.homyakin.seeker.telegram.command.user.item;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.UserCommand;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record InputLoadoutName(
    UserId userId,
    String name
) implements UserCommand {
    public static InputLoadoutName from(Message message) {
        return new InputLoadoutName(
            UserId.from(message.getFrom().getId()),
            message.getText()
        );
    }
}
