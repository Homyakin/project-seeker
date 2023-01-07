package ru.homyakin.seeker.telegram.command.group.profile;

import ru.homyakin.seeker.telegram.command.Command;

public record GetProfileInGroup(
    Long groupId,
    Long userId
) implements Command {
}
