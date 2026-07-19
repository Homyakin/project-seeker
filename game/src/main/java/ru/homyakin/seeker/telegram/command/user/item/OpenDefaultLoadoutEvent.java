package ru.homyakin.seeker.telegram.command.user.item;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.game.event.models.EventType;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.UserCommand;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record OpenDefaultLoadoutEvent(
    UserId userId,
    int messageId,
    EventType eventType
) implements UserCommand {
    public static OpenDefaultLoadoutEvent from(CallbackQuery callback) {
        final var parts = callback.getData().split(TextConstants.CALLBACK_DELIMITER);
        return new OpenDefaultLoadoutEvent(
            UserId.from(callback.getFrom().getId()),
            callback.getMessage().getMessageId(),
            EventType.valueOf(parts[1])
        );
    }
}
