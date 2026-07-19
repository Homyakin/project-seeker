package ru.homyakin.seeker.telegram.command.user.item;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.game.event.models.EventType;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.UserCommand;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record ToggleDefaultLoadout(
    UserId userId,
    int messageId,
    EventType eventType,
    long loadoutId
) implements UserCommand {
    public static ToggleDefaultLoadout from(CallbackQuery callback) {
        final var parts = callback.getData().split(TextConstants.CALLBACK_DELIMITER);
        return new ToggleDefaultLoadout(
            UserId.from(callback.getFrom().getId()),
            callback.getMessage().getMessageId(),
            EventType.valueOf(parts[1]),
            Long.parseLong(parts[2])
        );
    }
}
