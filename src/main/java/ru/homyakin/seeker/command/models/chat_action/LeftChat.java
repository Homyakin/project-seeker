package ru.homyakin.seeker.command.models.chat_action;

public record LeftChat(
    Long chatId
) implements ChatActionCommand {
}
