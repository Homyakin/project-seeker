package ru.homyakin.seeker.command.user;

import ru.homyakin.seeker.command.Command;

public record StartUser(
    Long userId
) implements Command {
}
