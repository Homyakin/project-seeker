package ru.homyakin.seeker.telegram.command.user.group;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.UserCommand;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record LeaveGroupInPrivateCancel(
    UserId userId,
    String callbackId,
    int messageId,
    PersonageId personageId
) implements UserCommand {
    public static LeaveGroupInPrivateCancel from(CallbackQuery callback) {
        return new LeaveGroupInPrivateCancel(
            UserId.from(callback.getFrom().getId()),
            callback.getId(),
            callback.getMessage().getMessageId(),
            PersonageId.from(callback.getData().split(TextConstants.CALLBACK_DELIMITER)[1])
        );
    }
}

