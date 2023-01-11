package ru.homyakin.seeker.telegram.command.user.level;

import ru.homyakin.seeker.telegram.command.Command;

public record LevelUp(
    long userId
) implements Command {
}
