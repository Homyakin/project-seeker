package ru.homyakin.seeker.telegram.command.group.language;

import ru.homyakin.seeker.telegram.command.Command;

public record GroupChangeLanguage(
    Long groupId
) implements Command {
}
