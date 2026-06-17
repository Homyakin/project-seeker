package ru.homyakin.seeker.telegram.command.group.management;

import ru.homyakin.seeker.telegram.command.UserGroupCommand;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record JoinGroupMember(
    GroupTgId groupTgId,
    UserId userId
) implements UserGroupCommand {
    public static JoinGroupMember from(Message message) {
        return new JoinGroupMember(GroupTgId.from(message.getChatId()), UserId.from(message.getFrom().getId()));
    }
}
