package ru.homyakin.seeker.event.launch.chat;

public record ChatEvent(
    Long launchedEventId,
    Long chatId,
    Integer messageId
) {
}
