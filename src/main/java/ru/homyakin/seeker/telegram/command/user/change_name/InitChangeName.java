package ru.homyakin.seeker.telegram.command.user.change_name;

import org.telegram.telegrambots.meta.api.objects.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record InitChangeName(
    UserId userId
) implements Command {
    public static InitChangeName from(Message message) {
        return new InitChangeName(UserId.from(message.getFrom().getId()));
    }
}
