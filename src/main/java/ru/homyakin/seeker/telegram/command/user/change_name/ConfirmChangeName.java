package ru.homyakin.seeker.telegram.command.user.change_name;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.UserCommand;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record ConfirmChangeName(
    UserId userId,
    String name
) implements UserCommand {
    public static ConfirmChangeName from(Message message, String name) {
        return new ConfirmChangeName(UserId.from(message.getFrom().getId()), name);
    }
}
