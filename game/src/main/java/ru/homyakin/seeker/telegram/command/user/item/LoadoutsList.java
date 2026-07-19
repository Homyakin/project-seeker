package ru.homyakin.seeker.telegram.command.user.item;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.telegram.command.UserCommand;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record LoadoutsList(
    UserId userId,
    int messageId
) implements UserCommand {
    public static LoadoutsList from(CallbackQuery callback) {
        return new LoadoutsList(
            UserId.from(callback.getFrom().getId()),
            callback.getMessage().getMessageId()
        );
    }
}
