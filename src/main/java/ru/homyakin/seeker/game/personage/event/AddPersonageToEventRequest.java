package ru.homyakin.seeker.game.personage.event;

import ru.homyakin.seeker.game.event.launched.EventPersonageParams;
import ru.homyakin.seeker.game.personage.models.PersonageId;

import java.util.Optional;

public record AddPersonageToEventRequest(
    long launchedEventId,
    PersonageId personageId,
    Optional<EventPersonageParams> personageParams
) {
}
