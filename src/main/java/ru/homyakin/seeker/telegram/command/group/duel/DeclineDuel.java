package ru.homyakin.seeker.telegram.command.group.duel;

import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.command.CommandType;

public record DeclineDuel(
    String callbackId,
    long groupId,
    long userId,
    int messageId,
    String data
) implements Command {
    public Long duelId() {
        return Long.valueOf(data.split(CommandType.CALLBACK_DELIMITER)[1]);
    }
}
