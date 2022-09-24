package ru.homyakin.seeker.command.user.language;

import ru.homyakin.seeker.command.Command;
import ru.homyakin.seeker.command.CommandText;

public record UserSelectLanguage(
    String callbackId,
    Long userId,
    Integer messageId,
    String data
) implements Command {
    public Integer getLanguageId() {
        return Integer.valueOf(data.split(CommandText.CALLBACK_DELIMITER)[1]);
    }
}
