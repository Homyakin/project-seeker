package ru.homyakin.seeker.command.chat.language;

import ru.homyakin.seeker.command.Command;
import ru.homyakin.seeker.command.CommandText;

public record GroupSelectLanguage(
    String callbackId,
    Long chatId,
    Integer messageId,
    Long userId,
    String data
) implements Command {
    public Integer getLanguageId() {
        return Integer.valueOf(data.split(CommandText.CALLBACK_DELIMITER)[1]);
    }
}
