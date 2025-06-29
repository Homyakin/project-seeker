package ru.homyakin.seeker.telegram.command.group.badge;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.common.models.BadgeId;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record SelectGroupBadge(
    UserId userId,
    GroupTgId groupTgId,
    Integer messageId,
    String callbackId,
    BadgeId badgeId
) implements Command {
    public static SelectGroupBadge from(CallbackQuery callback) {
        return new SelectGroupBadge(
            UserId.from(callback.getFrom().getId()),
            GroupTgId.from(callback.getMessage().getChatId()),
            callback.getMessage().getMessageId(),
            callback.getId(),
            BadgeId.of(Integer.parseInt(callback.getData().split(TextConstants.CALLBACK_DELIMITER)[1]))
        );
    }
}
