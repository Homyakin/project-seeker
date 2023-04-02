package ru.homyakin.seeker.telegram.command.group.settings;

import org.telegram.telegrambots.meta.api.objects.Message;
import ru.homyakin.seeker.telegram.command.Command;

public record GetActiveTime(
    long groupId
) implements Command {
    public static GetActiveTime from(Message message) {
        return new GetActiveTime(message.getChatId());
    }
}
