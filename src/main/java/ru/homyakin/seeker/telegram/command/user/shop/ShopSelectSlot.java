package ru.homyakin.seeker.telegram.command.user.shop;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.UserCommand;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record ShopSelectSlot(UserId userId, int messageId, String callbackId, PersonageSlot slot) implements UserCommand {
    public static ShopSelectSlot from(CallbackQuery callback) {
        final var parts = callback.getData().split(TextConstants.CALLBACK_DELIMITER);
        return new ShopSelectSlot(
            UserId.from(callback.getFrom().getId()),
            callback.getMessage().getMessageId(),
            callback.getId(),
            PersonageSlot.findById(Integer.parseInt(parts[1]))
        );
    }
}
