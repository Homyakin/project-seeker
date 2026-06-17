package ru.homyakin.seeker.telegram.command.group.top;

import ru.homyakin.seeker.telegram.command.UserGroupCommand;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record TopWorkerOfDay(GroupTgId groupTgId, UserId userId) implements UserGroupCommand {
    public static TopWorkerOfDay from(Message message) {
        return new TopWorkerOfDay(GroupTgId.from(message.getChatId()), UserId.from(message.getFrom().getId()));
    }
}
