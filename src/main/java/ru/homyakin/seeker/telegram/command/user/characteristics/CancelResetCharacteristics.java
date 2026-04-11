package ru.homyakin.seeker.telegram.command.user.characteristics;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.telegram.command.UserCommand;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record CancelResetCharacteristics(UserId userId, int messageId) implements UserCommand {
    public static CancelResetCharacteristics from(CallbackQuery callback) {
        return new CancelResetCharacteristics(
            UserId.from(callback.getFrom().getId()),
            callback.getMessage().getMessageId()
        );
    }
}
