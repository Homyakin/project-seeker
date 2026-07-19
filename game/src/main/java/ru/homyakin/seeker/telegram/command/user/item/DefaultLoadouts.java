package ru.homyakin.seeker.telegram.command.user.item;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.telegram.command.UserCommand;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record DefaultLoadouts(
    UserId userId,
    int messageId
) implements UserCommand {
    public static DefaultLoadouts from(CallbackQuery callback) {
        return new DefaultLoadouts(
            UserId.from(callback.getFrom().getId()),
            callback.getMessage().getMessageId()
        );
    }
}
