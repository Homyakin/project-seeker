package ru.homyakin.seeker.command.models.chat_action;

public record JoinChat(
    Long chatId
) implements ChatActionCommand {
}
