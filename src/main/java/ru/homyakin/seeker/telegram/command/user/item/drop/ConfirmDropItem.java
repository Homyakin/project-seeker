package ru.homyakin.seeker.telegram.command.user.item.drop;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record ConfirmDropItem(
    Integer messageId,
    UserId userId,
    long itemId
) implements Command {
    public static ConfirmDropItem from(CallbackQuery callback) {
        return new ConfirmDropItem(
            callback.getMessage().getMessageId(),
            UserId.from(callback.getFrom().getId()),
            Long.parseLong(callback.getData().split(TextConstants.CALLBACK_DELIMITER)[1])
        );
    }
}
