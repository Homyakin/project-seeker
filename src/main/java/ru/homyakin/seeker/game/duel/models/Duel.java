package ru.homyakin.seeker.game.duel.models;

import java.time.LocalDateTime;
import ru.homyakin.seeker.game.personage.models.PersonageId;

public record Duel(
    long id,
    PersonageId initiatingPersonageId,
    PersonageId acceptingPersonageId,
    LocalDateTime expiringDate,
    DuelStatus status
) {
    public boolean isFinalStatus() {
        return status != DuelStatus.WAITING;
    }
}
