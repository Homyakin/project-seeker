package ru.homyakin.seeker.telegram.command.user.characteristics;

import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.Command;

public record IncreaseCharacteristic(
    long userId,
    int messageId,
    String data
) implements Command {
    public String characteristicType() {
        return data.split(TextConstants.CALLBACK_DELIMITER)[1];
    }
}
