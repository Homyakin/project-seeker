package ru.homyakin.seeker.game.duel.models;

import java.time.LocalDateTime;
import java.util.Optional;
import ru.homyakin.seeker.game.event.models.EventStatus;
import ru.homyakin.seeker.game.personage.models.PersonageId;

public record Duel(
    long id,
    PersonageId initiatingPersonageId,
    PersonageId acceptingPersonageId,
    Optional<PersonageId> winnerPersonageId,
    LocalDateTime expiringDate,
    EventStatus status
) {
    public boolean isFinalStatus() {
        return status.isFinal();
    }
}
