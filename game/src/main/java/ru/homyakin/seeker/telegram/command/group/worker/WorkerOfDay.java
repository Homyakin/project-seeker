package ru.homyakin.seeker.telegram.command.group.worker;

import ru.homyakin.seeker.telegram.command.UserGroupCommand;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record WorkerOfDay(GroupTgId groupTgId, UserId userId) implements UserGroupCommand {
    public static WorkerOfDay from(Message message) {
        return new WorkerOfDay(GroupTgId.from(message.getChatId()), UserId.from(message.getFrom().getId()));
    }
}
