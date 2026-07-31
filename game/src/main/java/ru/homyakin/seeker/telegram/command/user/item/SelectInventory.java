package ru.homyakin.seeker.telegram.command.user.item;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.UserCommand;
import ru.homyakin.seeker.telegram.user.models.UserId;
import ru.homyakin.seeker.utils.CommonUtils;

public record SelectInventory(
    UserId userId,
    int messageId,
    InventorySection section,
    int page
) implements UserCommand {
    public static SelectInventory from(CallbackQuery callback) {
        final var parts = callback.getData().split(TextConstants.CALLBACK_DELIMITER);
        final var page = parts.length > 2
            ? CommonUtils.parseIntOrEmpty(parts[2]).orElse(0)
            : 0;
        return new SelectInventory(
            UserId.from(callback.getFrom().getId()),
            callback.getMessage().getMessageId(),
            InventorySection.findForce(parts[1]),
            page
        );
    }
}
