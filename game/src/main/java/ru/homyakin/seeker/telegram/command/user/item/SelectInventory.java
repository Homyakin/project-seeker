package ru.homyakin.seeker.telegram.command.user.item;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.UserCommand;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record SelectInventory(
    UserId userId,
    int messageId,
    InventorySection section
) implements UserCommand {
    public static SelectInventory from(CallbackQuery callback) {
        return new SelectInventory(
            UserId.from(callback.getFrom().getId()),
            callback.getMessage().getMessageId(),
            InventorySection.findForce(callback.getData().split(TextConstants.CALLBACK_DELIMITER)[1])
        );
    }
}
