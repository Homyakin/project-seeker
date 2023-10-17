package ru.homyakin.seeker.telegram.command.user.change_name;

import org.telegram.telegrambots.meta.api.objects.Message;
import ru.homyakin.seeker.telegram.command.Command;

public record InitChangeName(
    long userId
) implements Command {
    public static InitChangeName from(Message message) {
        return new InitChangeName(message.getFrom().getId());
    }
}
