package ru.homyakin.seeker.game.contraband.entity;

import ru.homyakin.seeker.game.personage.models.PersonageId;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ContrabandStorage {
    long create(Contraband contraband);

    Optional<Contraband> getById(long id);

    void update(Contraband contraband);

    Optional<Contraband> findActiveForPersonage(PersonageId personageId);

    List<Contraband> findPendingForBlackMarket();

    List<Contraband> findExpired(LocalDateTime now);

    int countFinderFailedOpensSinceLastSuccess(PersonageId personageId);

    int countReceiverFailedOpensSinceLastSuccess(PersonageId personageId);
}
