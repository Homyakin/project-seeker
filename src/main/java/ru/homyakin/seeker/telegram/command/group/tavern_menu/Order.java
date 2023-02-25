package ru.homyakin.seeker.telegram.command.group.tavern_menu;

import ru.homyakin.seeker.telegram.command.Command;

public record Order(
    long groupId,
    long userId,
    String text
) implements Command {
}
