package ru.homyakin.seeker.telegram.command.group.action;

import org.telegram.telegrambots.meta.api.objects.ChatMemberUpdated;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.group.models.GroupId;

public record JoinGroup(
    GroupId groupId
) implements Command {
    public static JoinGroup from(ChatMemberUpdated chatMember) {
        return new JoinGroup(GroupId.from(chatMember.getChat().getId()));
    }
}
