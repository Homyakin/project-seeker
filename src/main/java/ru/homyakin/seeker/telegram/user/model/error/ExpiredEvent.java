package ru.homyakin.seeker.telegram.user.model.error;

import ru.homyakin.seeker.event.models.Event;

public record ExpiredEvent(
    Event event
) implements EventError {
}
