package ru.homyakin.seeker.telegram.command.user.characteristics;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.telegram.command.Command;

public record CancelResetCharacteristics(long userId, int messageId) implements Command {
    public static CancelResetCharacteristics from(CallbackQuery callback) {
        return new CancelResetCharacteristics(
            callback.getFrom().getId(),
            callback.getMessage().getMessageId()
        );
    }
}
