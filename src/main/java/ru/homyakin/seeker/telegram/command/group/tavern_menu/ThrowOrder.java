package ru.homyakin.seeker.telegram.command.group.tavern_menu;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.group.models.GroupId;
import ru.homyakin.seeker.telegram.models.MentionInfo;
import ru.homyakin.seeker.telegram.user.models.UserId;

import java.util.Optional;

public record ThrowOrder(
    GroupId groupId,
    UserId userId,
    Optional<MentionInfo> mentionInfo
) implements Command {
    public static ThrowOrder from(Message message) {
        return new ThrowOrder(
            GroupId.from(message.getChatId()),
            UserId.from(message.getFrom().getId()),
            MentionInfo.from(message)
        );
    }
}
