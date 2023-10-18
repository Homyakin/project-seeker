package ru.homyakin.seeker.telegram.command.user.characteristics;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.game.personage.models.CharacteristicType;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.user.models.UserId;

public record IncreaseCharacteristic(
    UserId userId,
    int messageId,
    CharacteristicType characteristicType
) implements Command {
    public static IncreaseCharacteristic from(CallbackQuery callback) {
        return new IncreaseCharacteristic(
            UserId.from(callback.getFrom().getId()),
            callback.getMessage().getMessageId(),
            CharacteristicType.findForce(callback.getData().split(TextConstants.CALLBACK_DELIMITER)[1])
        );
    }
}
