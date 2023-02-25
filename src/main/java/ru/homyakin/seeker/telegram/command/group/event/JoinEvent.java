package ru.homyakin.seeker.telegram.command.group.event;

import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.command.type.CommandType;

public record JoinEvent(
    String callbackId,
    Long groupId,
    Integer messageId,
    Long userId,
    String data
) implements Command {

    public Long getLaunchedEventId() {
        return Long.valueOf(data.split(CommandType.CALLBACK_DELIMITER)[1]);
    }
}
