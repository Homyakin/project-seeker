package ru.homyakin.seeker.game.event.launched;

import ru.homyakin.seeker.game.event.models.EventStatus;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

public record LaunchedEvent(
    long id,
    int eventId,
    LocalDateTime startDate,
    LocalDateTime endDate,
    EventStatus status,
    Optional<EventParams> eventParams
) {
    public boolean isInFinalStatus() {
        return status.isFinal();
    }

    public Duration duration() {
        return Duration.between(startDate, endDate);
    }
}
