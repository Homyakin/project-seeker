package ru.homyakin.seeker.command.language;

import ru.homyakin.seeker.command.Command;

public record GroupChangeLanguage(
    Long chatId
) implements Command {
}
