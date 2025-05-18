package ru.homyakin.seeker.telegram.command.user.shop;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record AddModifier(
    UserId userId,
    long itemId
) implements Command {
    public static AddModifier from(Message message) {
        return new AddModifier(
            UserId.from(message.getFrom().getId()),
            Long.parseLong(message.getText().split(TextConstants.TG_COMMAND_DELIMITER)[1])
        );
    }
}
