package ru.homyakin.seeker.telegram.command.user.group;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record LeaveGroupInPrivate(
    UserId userId
) implements Command {
    public static LeaveGroupInPrivate from(Message message) {
        return new LeaveGroupInPrivate(UserId.from(message.getFrom().getId()));
    }
}

