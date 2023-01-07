package ru.homyakin.seeker.telegram.command.group.top;

import ru.homyakin.seeker.telegram.command.Command;

public record Top(
    long groupId,
    long userId
) implements Command {
}
