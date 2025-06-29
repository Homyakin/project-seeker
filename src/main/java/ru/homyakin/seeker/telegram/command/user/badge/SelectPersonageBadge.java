package ru.homyakin.seeker.telegram.command.user.badge;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.common.models.BadgeId;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record SelectPersonageBadge(
    UserId userId,
    Integer messageId,
    String callbackId,
    BadgeId badgeId
) implements Command {
    public static SelectPersonageBadge from(CallbackQuery callback) {
        return new SelectPersonageBadge(
            UserId.from(callback.getFrom().getId()),
            callback.getMessage().getMessageId(),
            callback.getId(),
            BadgeId.of(Integer.parseInt(callback.getData().split(TextConstants.CALLBACK_DELIMITER)[1]))
        );
    }
}
