package ru.homyakin.seeker.game.event.models;

public record ChatLaunchedEvent(
    long launchedEventId,
    long chatId,
    int messageId
) {
}
