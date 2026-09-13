package ru.homyakin.seeker.telegram.command.user.shop;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.UserCommand;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record ConfirmStormEnhance(
    UserId userId,
    long itemId,
    int expectedLevel,
    long expectedRevision
) implements UserCommand {
    public static ConfirmStormEnhance from(Message message) {
        final var parts = message.getText().split(TextConstants.TG_COMMAND_DELIMITER);
        return new ConfirmStormEnhance(
            UserId.from(message.getFrom().getId()),
            Long.parseLong(parts[1]),
            parts.length > 2 ? Integer.parseInt(parts[2]) : -1,
            parts.length > 3 ? Long.parseLong(parts[3]) : -1
        );
    }
}
