package ru.homyakin.seeker.telegram.command.user.profile;

import org.telegram.telegrambots.meta.api.objects.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.command.type.CommandType;

public record ChangeName(
    long userId,
    String name
) implements Command {
    public static ChangeName from(Message message) {
        return new ChangeName(
            message.getChatId(),
            message.getText().replaceAll(CommandType.CHANGE_NAME.getText(), "").trim()
        );
    }
}
