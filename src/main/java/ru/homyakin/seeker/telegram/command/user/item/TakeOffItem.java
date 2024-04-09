package ru.homyakin.seeker.telegram.command.user.item;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record TakeOffItem(
    UserId userId,
    long itemId
) implements Command {
    public static TakeOffItem from(Message message) {
        return new TakeOffItem(
            UserId.from(message.getFrom().getId()),
            Long.parseLong(message.getText().split(TextConstants.TG_COMMAND_DELIMITER)[1])
        );
    }
}
