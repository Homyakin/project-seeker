package ru.homyakin.seeker.user.errors;

import ru.homyakin.seeker.event.models.Event;

public record ExpiredEvent(
    Event event
) implements EventError {
}
