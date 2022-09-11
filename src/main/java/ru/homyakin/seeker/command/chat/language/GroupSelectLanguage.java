package ru.homyakin.seeker.command.chat.language;

import ru.homyakin.seeker.command.Command;

public record GroupSelectLanguage(
    String callbackId,
    Long chatId,
    Integer messageId,
    Long userId,
    String data
) implements Command {
}
