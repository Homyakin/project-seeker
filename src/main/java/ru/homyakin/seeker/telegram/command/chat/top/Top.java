package ru.homyakin.seeker.telegram.command.chat.top;

import ru.homyakin.seeker.telegram.command.Command;

public record Top(
    long chatId,
    long userId
) implements Command {
}
