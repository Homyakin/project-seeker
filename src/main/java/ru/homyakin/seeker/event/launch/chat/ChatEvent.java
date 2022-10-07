package ru.homyakin.seeker.event.launch.chat;

public record ChatEvent(
    long launchedEventId,
    long chatId,
    int messageId
) {
}
