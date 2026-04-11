package ru.homyakin.seeker.telegram.command.user.outpost;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.telegram.command.UserCommand;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record OpenOutpostMenuInline(UserId userId, int messageId, String callbackId) implements UserCommand {
    public static OpenOutpostMenuInline from(CallbackQuery callback) {
        return new OpenOutpostMenuInline(
            UserId.from(callback.getFrom().getId()),
            callback.getMessage().getMessageId(),
            callback.getId()
        );
    }
}
