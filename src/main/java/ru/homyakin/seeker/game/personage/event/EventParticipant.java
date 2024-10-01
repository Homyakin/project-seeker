package ru.homyakin.seeker.game.personage.event;

import ru.homyakin.seeker.game.event.launched.EventPersonageParams;
import ru.homyakin.seeker.game.personage.models.Personage;

import java.util.Optional;

public record EventParticipant(
    Personage personage,
    Optional<EventPersonageParams> params
) {
}
