package ru.homyakin.seeker.game.contraband.entity;

import ru.homyakin.seeker.game.personage.models.PersonageId;

import java.time.Duration;
import java.util.Optional;

public interface FindReceiver {
    Optional<PersonageId> findReceiver(Contraband contraband, Duration activityDuration);
}
