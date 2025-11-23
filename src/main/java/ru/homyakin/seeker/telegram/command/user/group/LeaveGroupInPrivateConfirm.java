package ru.homyakin.seeker.telegram.command.user.group;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record LeaveGroupInPrivateConfirm(
    UserId userId,
    String callbackId,
    int messageId,
    PersonageId personageId
) implements Command {
    public static LeaveGroupInPrivateConfirm from(CallbackQuery callback) {
        return new LeaveGroupInPrivateConfirm(
            UserId.from(callback.getFrom().getId()),
            callback.getId(),
            callback.getMessage().getMessageId(),
            PersonageId.from(callback.getData().split(TextConstants.CALLBACK_DELIMITER)[1])
        );
    }
}

