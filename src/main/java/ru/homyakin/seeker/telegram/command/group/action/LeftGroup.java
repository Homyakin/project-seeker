package ru.homyakin.seeker.telegram.command.group.action;

import ru.homyakin.seeker.telegram.command.Command;

public record LeftGroup(
    Long groupId
) implements Command {
}
