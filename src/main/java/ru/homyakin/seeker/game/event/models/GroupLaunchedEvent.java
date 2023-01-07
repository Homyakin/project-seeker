package ru.homyakin.seeker.game.event.models;

public record GroupLaunchedEvent(
    long launchedEventId,
    long groupId,
    int messageId
) {
}
