package ru.homyakin.seeker.telegram.command.common.help;

import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.Command;

public record SelectHelp(
    long chatId,
    int messageId,
    boolean isPrivate,
    String data
) implements Command {
    public String helpSection() {
        return data.split(TextConstants.CALLBACK_DELIMITER)[1];
    }
}
