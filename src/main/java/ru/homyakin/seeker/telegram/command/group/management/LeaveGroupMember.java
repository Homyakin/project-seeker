package ru.homyakin.seeker.telegram.command.group.management;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record LeaveGroupMember(
    GroupTgId groupTgId,
    UserId userId
) implements Command {
    public static LeaveGroupMember from(Message message) {
        return new LeaveGroupMember(GroupTgId.from(message.getChatId()), UserId.from(message.getFrom().getId()));
    }
}
