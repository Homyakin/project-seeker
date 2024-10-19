package ru.homyakin.seeker.telegram.command.group.top;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record TopSpin(GroupTgId groupId, UserId userId) implements Command {
    public static TopSpin from(Message message) {
        return new TopSpin(GroupTgId.from(message.getChatId()), UserId.from(message.getFrom().getId()));
    }
}
