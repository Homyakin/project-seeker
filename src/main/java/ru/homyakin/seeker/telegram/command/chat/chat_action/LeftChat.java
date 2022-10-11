package ru.homyakin.seeker.telegram.command.chat.chat_action;

import ru.homyakin.seeker.telegram.command.Command;

public record LeftChat(
    Long chatId
) implements Command {
}
