package ru.homyakin.seeker.telegram.command.group.spin;

import org.telegram.telegrambots.meta.api.objects.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.group.models.GroupId;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record SpinTop(GroupId groupId, UserId userId) implements Command {
    public static SpinTop from(Message message) {
        return new SpinTop(GroupId.from(message.getChatId()), UserId.from(message.getFrom().getId()));
    }
}
