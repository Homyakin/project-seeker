package ru.homyakin.seeker.telegram.command.group.action;

import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberUpdated;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;

public record JoinGroup(
    GroupTgId groupId
) implements Command {
    public static JoinGroup from(ChatMemberUpdated chatMember) {
        return new JoinGroup(GroupTgId.from(chatMember.getChat().getId()));
    }
}
