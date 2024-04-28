package ru.homyakin.seeker.telegram.command.group.settings;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.group.models.GroupId;

public record GetGroupSettings(
    GroupId groupId
) implements Command {
    public static GetGroupSettings from(Message message) {
        return new GetGroupSettings(GroupId.from(message.getChatId()));
    }
}
