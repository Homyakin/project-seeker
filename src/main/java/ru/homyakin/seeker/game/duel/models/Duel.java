package ru.homyakin.seeker.game.duel.models;

import java.time.LocalDateTime;
import java.util.Optional;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.telegram.group.models.GroupId;

public record Duel(
    long id,
    PersonageId initiatingPersonageId,
    PersonageId acceptingPersonageId,
    GroupId groupId,
    LocalDateTime expiringDate,
    DuelStatus status,
    Optional<Integer> messageId
) {
}
