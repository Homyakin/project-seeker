package ru.homyakin.seeker.telegram.command.user.badge;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record SelectBadge(
    UserId userId,
    Integer messageId,
    String callbackId,
    Integer badgeId
) implements Command {
    public static SelectBadge from(CallbackQuery callback) {
        return new SelectBadge(
            UserId.from(callback.getFrom().getId()),
            callback.getMessage().getMessageId(),
            callback.getId(),
            Integer.parseInt(callback.getData().split(TextConstants.CALLBACK_DELIMITER)[1])
        );
    }
}
