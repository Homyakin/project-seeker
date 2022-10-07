package ru.homyakin.seeker.event.models;

import java.time.LocalDateTime;

public record LaunchedEvent(
    long id,
    int eventId,
    LocalDateTime startDate,
    LocalDateTime endDate,
    boolean isActive
) {
}
