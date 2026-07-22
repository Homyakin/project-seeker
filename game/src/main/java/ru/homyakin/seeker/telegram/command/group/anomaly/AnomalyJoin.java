package ru.homyakin.seeker.telegram.command.group.anomaly;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.UserGroupCommand;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record AnomalyJoin(
    String callbackId,
    GroupTgId groupTgId,
    Integer messageId,
    UserId userId,
    long launchedEventId
) implements UserGroupCommand {
    public static AnomalyJoin from(CallbackQuery callback) {
        return new AnomalyJoin(
            callback.getId(),
            GroupTgId.from(callback.getMessage().getChatId()),
            callback.getMessage().getMessageId(),
            UserId.from(callback.getFrom().getId()),
            Long.parseLong(callback.getData().split(TextConstants.CALLBACK_DELIMITER)[1])
        );
    }
}
