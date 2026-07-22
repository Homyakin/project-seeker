package ru.homyakin.seeker.telegram.command.group.anomaly;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyMode;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.UserGroupCommand;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record AnomalyChooseMode(
    String callbackId,
    GroupTgId groupTgId,
    Integer messageId,
    UserId userId,
    long launchedEventId,
    AnomalyMode mode
) implements UserGroupCommand {
    public static AnomalyChooseMode safe(CallbackQuery callback) {
        return from(callback, AnomalyMode.SAFE);
    }

    public static AnomalyChooseMode dangerous(CallbackQuery callback) {
        return from(callback, AnomalyMode.DANGEROUS);
    }

    private static AnomalyChooseMode from(CallbackQuery callback, AnomalyMode mode) {
        return new AnomalyChooseMode(
            callback.getId(),
            GroupTgId.from(callback.getMessage().getChatId()),
            callback.getMessage().getMessageId(),
            UserId.from(callback.getFrom().getId()),
            Long.parseLong(callback.getData().split(TextConstants.CALLBACK_DELIMITER)[1]),
            mode
        );
    }
}
