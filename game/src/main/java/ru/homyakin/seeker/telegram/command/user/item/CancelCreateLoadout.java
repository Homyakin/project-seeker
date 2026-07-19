package ru.homyakin.seeker.telegram.command.user.item;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.telegram.command.UserCommand;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record CancelCreateLoadout(
    UserId userId,
    int messageId,
    String callbackId
) implements UserCommand {
    public static CancelCreateLoadout from(CallbackQuery callback) {
        return new CancelCreateLoadout(
            UserId.from(callback.getFrom().getId()),
            callback.getMessage().getMessageId(),
            callback.getId()
        );
    }
}
