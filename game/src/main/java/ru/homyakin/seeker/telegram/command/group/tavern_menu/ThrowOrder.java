package ru.homyakin.seeker.telegram.command.group.tavern_menu;

import ru.homyakin.seeker.telegram.command.UserGroupCommand;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
import ru.homyakin.seeker.telegram.models.MentionInfo;
import ru.homyakin.seeker.telegram.user.models.UserId;

import java.util.Optional;

public record ThrowOrder(
    GroupTgId groupTgId,
    UserId userId,
    Optional<MentionInfo> mentionInfo
) implements UserGroupCommand {
    public static ThrowOrder from(Message message) {
        return new ThrowOrder(
            GroupTgId.from(message.getChatId()),
            UserId.from(message.getFrom().getId()),
            MentionInfo.from(message)
        );
    }
}
