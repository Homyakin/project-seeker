package ru.homyakin.seeker.command.chat.chat_action;

import ru.homyakin.seeker.command.Command;

public record JoinChat(
    Long chatId
) implements Command {
}
