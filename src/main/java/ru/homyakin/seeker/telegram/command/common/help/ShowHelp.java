package ru.homyakin.seeker.telegram.command.common.help;

import ru.homyakin.seeker.telegram.command.Command;

public record ShowHelp(
    long chatId,
    boolean isPrivate
) implements Command {
}
