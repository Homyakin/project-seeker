package ru.homyakin.seeker.telegram.command.group.top;

import org.telegram.telegrambots.meta.api.objects.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.group.models.GroupId;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record TopSpin(GroupId groupId, UserId userId) implements Command {
    public static TopSpin from(Message message) {
        return new TopSpin(GroupId.from(message.getChatId()), UserId.from(message.getFrom().getId()));
    }
}
