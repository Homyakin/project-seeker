package ru.homyakin.seeker.telegram.command.group.action;

import ru.homyakin.seeker.telegram.command.Command;

public record JoinGroup(
    Long groupId
) implements Command {
}
