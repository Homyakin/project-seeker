package ru.homyakin.seeker.game.event.models;

public record ChatEvent(
    long launchedEventId,
    long chatId,
    int messageId
) {
}
