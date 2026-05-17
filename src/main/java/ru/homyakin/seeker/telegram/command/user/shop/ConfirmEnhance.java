package ru.homyakin.seeker.telegram.command.user.shop;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.UserCommand;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record ConfirmEnhance(
    UserId userId,
    long itemId
) implements UserCommand {
    public static ConfirmEnhance from(Message message) {
        return new ConfirmEnhance(
            UserId.from(message.getFrom().getId()),
            Long.parseLong(message.getText().split(TextConstants.TG_COMMAND_DELIMITER)[1])
        );
    }
}
