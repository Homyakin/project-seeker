package ru.homyakin.seeker.game.personage.models;

import ru.homyakin.seeker.game.event.models.EventType;

import java.time.LocalDateTime;

public record CurrentEvent(
    EventType type,
    LocalDateTime endDate
) {
}
