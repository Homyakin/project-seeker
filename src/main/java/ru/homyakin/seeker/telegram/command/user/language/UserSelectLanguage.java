package ru.homyakin.seeker.telegram.command.user.language;

import ru.homyakin.seeker.telegram.command.type.CommandType;
import ru.homyakin.seeker.telegram.command.Command;

public record UserSelectLanguage(
    String callbackId,
    Long userId,
    Integer messageId,
    String data
) implements Command {
    public Integer getLanguageId() {
        return Integer.valueOf(data.split(CommandType.CALLBACK_DELIMITER)[1]);
    }
}
