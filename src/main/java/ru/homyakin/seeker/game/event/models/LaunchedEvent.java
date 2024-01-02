package ru.homyakin.seeker.game.event.models;

import java.time.LocalDateTime;

public record LaunchedEvent(
    long id,
    int eventId,
    LocalDateTime startDate,
    LocalDateTime endDate,
    EventStatus status
) {
    public boolean isInFinalStatus() {
        return status.isFinal();
    }

    public boolean isNotInFinalStatus() {
        return !isInFinalStatus();
    }
}
