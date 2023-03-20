package ru.homyakin.seeker.telegram.command.user.characteristics;

import ru.homyakin.seeker.telegram.command.Command;

public record CancelResetCharacteristics(
    long userId,
    int messageId
) implements Command {
}
