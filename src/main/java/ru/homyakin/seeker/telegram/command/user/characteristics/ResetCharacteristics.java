package ru.homyakin.seeker.telegram.command.user.characteristics;

import ru.homyakin.seeker.telegram.command.Command;

public record ResetCharacteristics(long userId) implements Command {
}
