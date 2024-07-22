package ru.homyakin.seeker.telegram.command.user.shop;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record SellItem(UserId userId, Long itemId) implements Command {
    public static SellItem from(Message message) {
        final var itemId = message.getText().split(TextConstants.TG_COMMAND_DELIMITER)[1];
        return new SellItem(
            UserId.from(message.getFrom().getId()),
            Long.parseLong(itemId)
        );
    }
}
