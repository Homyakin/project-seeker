package ru.homyakin.seeker.telegram.command.group.stats;

import org.telegram.telegrambots.meta.api.objects.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.group.models.GroupId;

public record GetGroupStats(GroupId groupId) implements Command {
    public static GetGroupStats from(Message message) {
        return new GetGroupStats(GroupId.from(message.getChatId()));
    }
}
