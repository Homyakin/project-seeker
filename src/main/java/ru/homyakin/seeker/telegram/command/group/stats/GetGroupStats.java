package ru.homyakin.seeker.telegram.command.group.stats;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;

public record GetGroupStats(GroupTgId groupId) implements Command {
    public static GetGroupStats from(Message message) {
        return new GetGroupStats(GroupTgId.from(message.getChatId()));
    }
}
