package ru.homyakin.seeker.telegram.command.user.level;

import ru.homyakin.seeker.telegram.command.Command;

public record CharacteristicUp(
    long userId,
    CharacteristicType characteristicType
) implements Command {
}
