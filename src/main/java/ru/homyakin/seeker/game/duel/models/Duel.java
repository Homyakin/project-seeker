package ru.homyakin.seeker.game.duel.models;

import java.time.LocalDateTime;
import java.util.Optional;
import ru.homyakin.seeker.game.duel.DuelStatus;

public record Duel(
    long id,
    long initiatingPersonageId,
    long acceptingPersonageId,
    long groupId,
    LocalDateTime expiringDate,
    DuelStatus status,
    Optional<Integer> messageId
) {
}
