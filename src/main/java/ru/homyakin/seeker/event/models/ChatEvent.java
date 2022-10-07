package ru.homyakin.seeker.event.models;

public record ChatEvent(
    long launchedEventId,
    long chatId,
    int messageId
) {
}
