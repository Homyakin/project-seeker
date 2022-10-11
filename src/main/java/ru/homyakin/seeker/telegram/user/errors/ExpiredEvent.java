package ru.homyakin.seeker.telegram.user.errors;

import ru.homyakin.seeker.event.models.Event;

public record ExpiredEvent(
    Event event
) implements EventError {
}
