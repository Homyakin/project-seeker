package ru.homyakin.seeker.telegram.command.group.tavern_menu;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.group.models.GroupId;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record ConsumeOrder(
    String callbackId,
    GroupId groupId,
    Integer messageId,
    UserId userId,
    Long orderId
) implements Command {
    public static ConsumeOrder from(CallbackQuery callback) {
        return new ConsumeOrder(
            callback.getId(),
            GroupId.from(callback.getMessage().getChatId()),
            callback.getMessage().getMessageId(),
            UserId.from(callback.getFrom().getId()),
            Long.parseLong(callback.getData().split(TextConstants.CALLBACK_DELIMITER)[1])
        );
    }
}
