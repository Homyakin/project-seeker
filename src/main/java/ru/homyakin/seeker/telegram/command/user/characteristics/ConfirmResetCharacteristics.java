package ru.homyakin.seeker.telegram.command.user.characteristics;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record ConfirmResetCharacteristics(UserId userId, int messageId) implements Command {
    public static ConfirmResetCharacteristics from(CallbackQuery callback) {
        return new ConfirmResetCharacteristics(
            UserId.from(callback.getFrom().getId()),
            callback.getMessage().getMessageId()
        );
    }
}
