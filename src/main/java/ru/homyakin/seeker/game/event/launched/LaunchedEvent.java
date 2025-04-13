package ru.homyakin.seeker.game.event.launched;

import ru.homyakin.seeker.game.event.models.EventStatus;

import java.time.Duration;
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

    public Duration duration() {
        return Duration.between(startDate, endDate);
    }
}
