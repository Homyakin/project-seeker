package ru.homyakin.seeker.telegram.command.group.action;

import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberUpdated;
import ru.homyakin.seeker.telegram.command.GroupCommand;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;

public record LeftGroup(
    GroupTgId groupTgId
) implements GroupCommand {
    public static LeftGroup from(ChatMemberUpdated chatMember) {
        return new LeftGroup(GroupTgId.from(chatMember.getChat().getId()));
    }
}
