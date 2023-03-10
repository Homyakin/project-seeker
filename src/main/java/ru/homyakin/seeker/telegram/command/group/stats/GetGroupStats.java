package ru.homyakin.seeker.telegram.command.group.stats;

import ru.homyakin.seeker.telegram.command.Command;

public record GetGroupStats(
    long groupId
) implements Command {
}
