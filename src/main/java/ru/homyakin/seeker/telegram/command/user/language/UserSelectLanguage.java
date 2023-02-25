package ru.homyakin.seeker.telegram.command.user.language;

import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.Command;

public record UserSelectLanguage(
    String callbackId,
    Long userId,
    Integer messageId,
    String data
) implements Command {
    public Integer getLanguageId() {
        return Integer.valueOf(data.split(TextConstants.CALLBACK_DELIMITER)[1]);
    }
}
