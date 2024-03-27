package ru.homyakin.seeker.telegram.command.user.item.drop;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record RejectDropItem(
    Integer messageId,
    UserId userId
) implements Command {
    public static RejectDropItem from(CallbackQuery callback) {
        return new RejectDropItem(
            callback.getMessage().getMessageId(),
            UserId.from(callback.getFrom().getId())
        );
    }
}
