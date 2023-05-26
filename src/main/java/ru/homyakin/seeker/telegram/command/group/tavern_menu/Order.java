package ru.homyakin.seeker.telegram.command.group.tavern_menu;

import java.util.Optional;

import org.telegram.telegrambots.meta.api.objects.Message;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.models.ReplyInfo;

public record Order(
    long groupId,
    long userId,
    int messageId,
    Optional<Integer> itemId,
    Optional<ReplyInfo> replyInfo
) implements Command {
    public static Order from(Message message) {
        return new Order(
            message.getChatId(),
            message.getFrom().getId(),
            message.getMessageId(),
            OrderUtils.getMenuItemId(message.getText()),
            ReplyInfo.from(message)
        );
    }
}
