package ru.homyakin.seeker.telegram.command.user.profile;

import ru.homyakin.seeker.telegram.command.Command;

public record GetProfileInPrivate(
    Long userId
) implements Command {
}
