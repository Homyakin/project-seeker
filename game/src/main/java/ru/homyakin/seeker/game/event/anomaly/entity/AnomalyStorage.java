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
     * Atomically moves a SEARCHING anomaly (no opponent yet) to CHALLENGED with the given opponent
     * and claims that opponent's one challenge tip-off for {@code challengeDay}.
     * Returns false if the row was already claimed, the opponent is already an active opponent elsewhere,
     * or the opponent already received a challenge tip-off on {@code challengeDay}.
     */
    boolean tryAssignOpponent(Anomaly.Dangerous.Challenged challenged, LocalDate challengeDay);

    /**
     * Atomically merges a guest SEARCHING expedition into the host:
     * host becomes ACCEPTED with the guest group as opponent; guest launched event is canceled;
     * participants and Telegram group-event links move to the host event;
     * guest group is attached to the host launched event.
     * Returns false if either side is no longer an unopposed SEARCHING expedition.
     */
    boolean tryMergeSearchingInto(Anomaly.Dangerous.Accepted hostAccepted, long guestLaunchedEventId);

    Optional<Anomaly> findByLaunchedEventId(long launchedEventId);

    Optional<LaunchedEvent> findActiveLaunchedEventByGroupId(GroupId groupId);

    List<LaunchedEvent> findActiveSearchingWithoutOpponent();

    boolean hasActiveAnomaly(GroupId groupId);

    boolean hasStartOnDate(GroupId groupId, LocalDate date);
}
