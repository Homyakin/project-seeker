package ru.homyakin.seeker.game.duel.models;

import java.time.LocalDateTime;
import java.util.Optional;
import ru.homyakin.seeker.telegram.group.models.GroupId;

public record Duel(
    long id,
    long initiatingPersonageId,
    long acceptingPersonageId,
    GroupId groupId,
    LocalDateTime expiringDate,
    DuelStatus status,
    Optional<Integer> messageId
) {
}
