package ru.homyakin.seeker.telegram.command.user.shop;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.telegram.command.UserCommand;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record ShopSelectRandomBoxes(UserId userId, int messageId, String callbackId) implements UserCommand {
    public static ShopSelectRandomBoxes from(CallbackQuery callback) {
        return new ShopSelectRandomBoxes(
            UserId.from(callback.getFrom().getId()),
            callback.getMessage().getMessageId(),
            callback.getId()
        );
    }
}
