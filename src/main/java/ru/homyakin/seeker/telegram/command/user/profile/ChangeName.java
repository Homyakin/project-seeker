package ru.homyakin.seeker.telegram.command.user.profile;

import ru.homyakin.seeker.telegram.command.Command;

public record ChangeName(
    long userId,
    String data
) implements Command {
}
