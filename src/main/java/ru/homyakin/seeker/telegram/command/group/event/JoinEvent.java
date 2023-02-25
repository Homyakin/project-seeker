package ru.homyakin.seeker.telegram.command.group.event;

import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.Command;

public record JoinEvent(
    String callbackId,
    Long groupId,
    Integer messageId,
    Long userId,
    String data
) implements Command {

    public Long getLaunchedEventId() {
        return Long.valueOf(data.split(TextConstants.CALLBACK_DELIMITER)[1]);
    }
}
