package ru.homyakin.seeker.telegram.command.group.tavern_menu;

import java.util.Optional;

import org.telegram.telegrambots.meta.api.objects.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.group.models.GroupId;
import ru.homyakin.seeker.telegram.models.MentionInfo;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record Order(
    GroupId groupId,
    UserId userId,
    int messageId,
    String itemCode,
    Optional<MentionInfo> mentionInfo
) implements Command {
    public static Order from(Message message) {
        return new Order(
            GroupId.from(message.getChatId()),
            UserId.from(message.getFrom().getId()),
            message.getMessageId(),
            OrderUtils.getMenuItemCode(message.getText()),
            MentionInfo.from(message)
        );
    }
}
