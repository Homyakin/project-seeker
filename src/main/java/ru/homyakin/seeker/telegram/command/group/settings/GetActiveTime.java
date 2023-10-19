package ru.homyakin.seeker.telegram.command.group.settings;

import org.telegram.telegrambots.meta.api.objects.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.group.models.GroupId;

public record GetActiveTime(
    GroupId groupId
) implements Command {
    public static GetActiveTime from(Message message) {
        return new GetActiveTime(GroupId.from(message.getChatId()));
    }
}
