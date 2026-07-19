package ru.homyakin.seeker.telegram.command.user.outpost;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.telegram.command.UserCommand;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record CancelOutpostDonate(
    UserId userId,
    int messageId,
    String callbackId
) implements UserCommand {
    public static CancelOutpostDonate from(CallbackQuery callback) {
        return new CancelOutpostDonate(
            UserId.from(callback.getFrom().getId()),
            callback.getMessage().getMessageId(),
            callback.getId()
        );
    }
}
