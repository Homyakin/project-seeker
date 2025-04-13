package ru.homyakin.seeker.game.event.launched;

import ru.homyakin.seeker.game.event.models.EventType;

import java.time.LocalDateTime;

public record CurrentEvent(
    long id,
    EventType type,
    LocalDateTime endDate
) {
}
