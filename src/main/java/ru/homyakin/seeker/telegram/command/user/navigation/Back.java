package ru.homyakin.seeker.telegram.command.user.navigation;

import ru.homyakin.seeker.telegram.command.Command;

public record Back(
    long userId
) implements Command {
}
