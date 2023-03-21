package ru.homyakin.seeker.telegram.command.user.characteristics;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.telegram.command.Command;

public record ConfirmResetCharacteristics(long userId, int messageId) implements Command {
    public static ConfirmResetCharacteristics from(CallbackQuery callback) {
        return new ConfirmResetCharacteristics(
            callback.getFrom().getId(),
            callback.getMessage().getMessageId()
        );
    }
}
