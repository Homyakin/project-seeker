package ru.homyakin.seeker.telegram.command.group.spin;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.group.models.GroupId;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record Spin(GroupId groupId, UserId userId) implements Command {
    public static Spin from(Message message) {
        return new Spin(GroupId.from(message.getChatId()), UserId.from(message.getFrom().getId()));
    }
}
