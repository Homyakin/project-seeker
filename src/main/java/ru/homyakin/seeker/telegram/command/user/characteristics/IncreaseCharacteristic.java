package ru.homyakin.seeker.telegram.command.user.characteristics;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.game.personage.models.CharacteristicType;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.Command;

public record IncreaseCharacteristic(
    long userId,
    int messageId,
    CharacteristicType characteristicType
) implements Command {
    public static IncreaseCharacteristic from(CallbackQuery callback) {
        return new IncreaseCharacteristic(
            callback.getFrom().getId(),
            callback.getMessage().getMessageId(),
            CharacteristicType.findForce(callback.getData().split(TextConstants.CALLBACK_DELIMITER)[1])
        );
    }
}
