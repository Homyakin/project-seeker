package ru.homyakin.seeker.telegram.command.user.shop;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.UserCommand;
import ru.homyakin.seeker.telegram.user.models.UserId;
import ru.homyakin.seeker.utils.CommonUtils;

public record ShopSelectRandomBoxes(UserId userId, int messageId, String callbackId, int page) implements UserCommand {
    public static ShopSelectRandomBoxes from(CallbackQuery callback) {
        final var parts = callback.getData().split(TextConstants.CALLBACK_DELIMITER);
        final var page = parts.length > 1
            ? CommonUtils.parseIntOrEmpty(parts[1]).orElse(0)
            : 0;
        return new ShopSelectRandomBoxes(
            UserId.from(callback.getFrom().getId()),
            callback.getMessage().getMessageId(),
            callback.getId(),
            page
        );
    }
}
