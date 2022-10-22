package ru.homyakin.seeker.telegram.command.common;

import ru.homyakin.seeker.telegram.command.Command;

public record Help(
    long chatId,
    boolean isPrivate
) implements Command {
}
