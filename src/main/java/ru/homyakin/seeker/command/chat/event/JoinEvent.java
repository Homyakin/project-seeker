package ru.homyakin.seeker.command.chat.event;

import ru.homyakin.seeker.command.Command;
import ru.homyakin.seeker.command.CommandText;

public record JoinEvent(
    String callbackId,
    Long chatId,
    Long userId,
    String data,
    Integer messageId,
    String messageText
) implements Command {

    public Long getLaunchedEventId() {
        return Long.valueOf(data.split(CommandText.CALLBACK_DELIMITER)[1]);
    }
}
