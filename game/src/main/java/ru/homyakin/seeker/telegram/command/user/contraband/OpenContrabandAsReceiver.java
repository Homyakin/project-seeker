package ru.homyakin.seeker.telegram.command.user.contraband;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.UserCommand;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record OpenContrabandAsReceiver(
    UserId userId,
    Integer messageId,
    String callbackId,
    long contrabandId
) implements UserCommand {
    public static OpenContrabandAsReceiver from(CallbackQuery callback) {
        return new OpenContrabandAsReceiver(
            UserId.from(callback.getFrom().getId()),
            callback.getMessage().getMessageId(),
            callback.getId(),
            Long.parseLong(callback.getData().split(TextConstants.CALLBACK_DELIMITER)[1])
        );
    }
}
