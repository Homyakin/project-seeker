package ru.homyakin.seeker.telegram.command.group.duel;

import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.Command;

public record AcceptDuel(
    String callbackId,
    long groupId,
    long userId,
    int messageId,
    String data
) implements Command {
    public Long duelId() {
        return Long.valueOf(data.split(TextConstants.CALLBACK_DELIMITER)[1]);
    }
}
