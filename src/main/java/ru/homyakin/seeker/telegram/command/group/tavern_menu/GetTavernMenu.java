package ru.homyakin.seeker.telegram.command.group.tavern_menu;

import ru.homyakin.seeker.telegram.command.Command;

public record GetTavernMenu(
    long groupId
) implements Command {
}
