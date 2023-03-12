package ru.homyakin.seeker.telegram.command.group.tavern_menu;

import ru.homyakin.seeker.telegram.command.Command;

public record Order(
    long groupId,
    long userId,
    int messageId,
    String text
) implements Command {
}
