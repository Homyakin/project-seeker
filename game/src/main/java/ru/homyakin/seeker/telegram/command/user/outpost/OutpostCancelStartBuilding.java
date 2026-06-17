package ru.homyakin.seeker.telegram.command.user.outpost;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.telegram.command.UserCommand;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record OutpostCancelStartBuilding(UserId userId, int messageId, String callbackId) implements UserCommand {
    public static OutpostCancelStartBuilding from(CallbackQuery callback) {
        return new OutpostCancelStartBuilding(
            UserId.from(callback.getFrom().getId()),
            callback.getMessage().getMessageId(),
            callback.getId()
        );
    }
}
