package ru.homyakin.seeker.game.personage.models.errors;

import ru.homyakin.seeker.game.event.models.Event;

public sealed interface PersonageEventError {
    enum EventInProcess implements PersonageEventError { INSTANCE }

    enum EventNotExist implements PersonageEventError { INSTANCE }

    enum PersonageInOtherEvent implements PersonageEventError { INSTANCE }

    enum PersonageInThisEvent implements PersonageEventError { INSTANCE }

    record ExpiredEvent(
        Event event
    ) implements PersonageEventError {
    }
}
