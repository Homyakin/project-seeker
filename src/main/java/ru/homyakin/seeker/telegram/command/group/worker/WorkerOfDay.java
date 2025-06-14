package ru.homyakin.seeker.telegram.command.group.worker;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record WorkerOfDay(GroupTgId groupId, UserId userId) implements Command {
    public static WorkerOfDay from(Message message) {
        return new WorkerOfDay(GroupTgId.from(message.getChatId()), UserId.from(message.getFrom().getId()));
    }
}
