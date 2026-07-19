package ru.homyakin.seeker.telegram.command.user.item;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.telegram.command.UserCommand;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record CreateLoadout(
    UserId userId,
    int messageId,
    String callbackId
) implements UserCommand {
    public static CreateLoadout from(CallbackQuery callback) {
        return new CreateLoadout(
            UserId.from(callback.getFrom().getId()),
            callback.getMessage().getMessageId(),
            callback.getId()
        );
    }
}
