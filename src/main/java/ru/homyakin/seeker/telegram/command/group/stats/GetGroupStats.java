package ru.homyakin.seeker.telegram.command.group.stats;

import org.telegram.telegrambots.meta.api.objects.Message;
import ru.homyakin.seeker.telegram.command.Command;

public record GetGroupStats(long groupId) implements Command {
    public static GetGroupStats from(Message message) {
        return new GetGroupStats(message.getChatId());
    }
}
