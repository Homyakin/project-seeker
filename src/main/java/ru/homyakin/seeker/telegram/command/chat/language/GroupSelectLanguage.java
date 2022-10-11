package ru.homyakin.seeker.telegram.command.chat.language;

import ru.homyakin.seeker.telegram.command.CommandText;
import ru.homyakin.seeker.telegram.command.Command;

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
