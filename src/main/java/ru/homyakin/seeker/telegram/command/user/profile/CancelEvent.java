package ru.homyakin.seeker.telegram.command.user.profile;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record CancelEvent(
    UserId userId,
    long launchedEventId
) implements Command {
    public static CancelEvent from(Message message) {
        final var launchedEventId = message.getText().split(TextConstants.TG_COMMAND_DELIMITER)[1];
        return new CancelEvent(
            UserId.from(message.getFrom().getId()),
            Long.parseLong(launchedEventId)
        );
    }
}
