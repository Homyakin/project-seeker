package ru.homyakin.seeker.telegram.command.group.action;

import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberUpdated;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.group.models.GroupId;

public record LeftGroup(
    GroupId groupId
) implements Command {
    public static LeftGroup from(ChatMemberUpdated chatMember) {
        return new LeftGroup(GroupId.from(chatMember.getChat().getId()));
    }
}
