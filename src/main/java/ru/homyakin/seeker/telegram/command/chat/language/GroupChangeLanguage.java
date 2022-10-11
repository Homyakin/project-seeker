package ru.homyakin.seeker.telegram.command.chat.language;

import ru.homyakin.seeker.telegram.command.Command;

public record GroupChangeLanguage(
    Long chatId
) implements Command {
}
