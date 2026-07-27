package ru.homyakin.seeker.game.event.anomaly.entity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.event.launched.LaunchedEvent;

public interface AnomalyStorage {
    void save(Anomaly anomaly);

    void update(Anomaly anomaly);

    /**
     * Atomically moves a SEARCHING anomaly (no opponent yet) to CHALLENGED with the given opponent.
     * Returns false if the row was already claimed, or the opponent is already an active opponent elsewhere.
     */
    boolean tryAssignOpponent(Anomaly.Dangerous.Challenged challenged);

    Optional<Anomaly> findByLaunchedEventId(long launchedEventId);

    Optional<LaunchedEvent> findActiveLaunchedEventByGroupId(GroupId groupId);

    List<LaunchedEvent> findActiveSearchingWithoutOpponent();

    boolean hasActiveAnomaly(GroupId groupId);

    boolean hasStartOnDate(GroupId groupId, LocalDate date);
}
