package ru.homyakin.seeker.telegram.command.user.item;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.UserCommand;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record ConfirmDeleteLoadout(
    UserId userId,
    int messageId,
    String callbackId,
    long loadoutId
) implements UserCommand {
    public static ConfirmDeleteLoadout from(CallbackQuery callback) {
        return new ConfirmDeleteLoadout(
            UserId.from(callback.getFrom().getId()),
            callback.getMessage().getMessageId(),
            callback.getId(),
            Long.parseLong(callback.getData().split(TextConstants.CALLBACK_DELIMITER)[1])
        );
    }
}
