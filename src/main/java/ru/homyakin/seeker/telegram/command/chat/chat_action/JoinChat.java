package ru.homyakin.seeker.telegram.command.chat.chat_action;

import ru.homyakin.seeker.telegram.command.Command;

public record JoinChat(
    Long chatId
) implements Command {
}
