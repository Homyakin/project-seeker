package ru.homyakin.seeker.telegram.command.user.characteristics;

import ru.homyakin.seeker.telegram.command.Command;

public record ConfirmResetCharacteristics(
    long userId,
    int messageId
) implements Command {
}
