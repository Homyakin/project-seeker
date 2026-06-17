package ru.homyakin.seeker.game.contraband.entity;

import ru.homyakin.seeker.game.personage.models.PersonageId;

import java.time.LocalDateTime;
import java.util.Optional;

public record Contraband(
    long id,
    ContrabandTier tier,
    PersonageId finderPersonageId,
    Optional<PersonageId> receiverPersonageId,
    ContrabandStatus status,
    LocalDateTime createdAt,
    LocalDateTime expiresAt,
    Optional<LocalDateTime> processedAt
) {
    public boolean isExpired(LocalDateTime now) {
        return now.isAfter(expiresAt);
    }

    public boolean canBeProcessedByFinder() {
        return status == ContrabandStatus.FOUND;
    }

    public boolean canBeProcessedByReceiver() {
        return status == ContrabandStatus.WAITING_RECEIVER;
    }

    public boolean canBeAccessedBy(PersonageId personageId) {
        if (finderPersonageId.equals(personageId) && canBeProcessedByFinder()) {
            return true;
        }
        return receiverPersonageId
            .map(receiverId -> receiverId.equals(personageId) && canBeProcessedByReceiver())
            .orElse(false);
    }

    public Contraband withStatus(ContrabandStatus newStatus, LocalDateTime processedAt) {
        return new Contraband(
            id, tier, finderPersonageId, receiverPersonageId,
            newStatus, createdAt, expiresAt, Optional.of(processedAt)
        );
    }

    public Contraband withReceiver(PersonageId receiverId, LocalDateTime newExpiresAt) {
        return new Contraband(
            id, tier, finderPersonageId, Optional.of(receiverId),
            ContrabandStatus.WAITING_RECEIVER, createdAt, newExpiresAt, processedAt
        );
    }
}
