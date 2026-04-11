package ru.homyakin.seeker.telegram.command.group.tavern_menu;

import ru.homyakin.seeker.telegram.command.UserGroupCommand;
import java.util.Optional;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
import ru.homyakin.seeker.telegram.models.MentionInfo;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record Order(
    GroupTgId groupTgId,
    UserId userId,
    int messageId,
    String itemCode,
    Optional<MentionInfo> mentionInfo
) implements UserGroupCommand {
    public static Order from(Message message) {
        return new Order(
            GroupTgId.from(message.getChatId()),
            UserId.from(message.getFrom().getId()),
            message.getMessageId(),
            OrderUtils.getMenuItemCode(message.getText()),
            MentionInfo.from(message)
        );
    }
}
