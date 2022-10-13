package ru.homyakin.seeker.game.personage.model.error;

import ru.homyakin.seeker.game.event.models.Event;

public record ExpiredEvent(
    Event event
) implements PersonageEventError {
}
