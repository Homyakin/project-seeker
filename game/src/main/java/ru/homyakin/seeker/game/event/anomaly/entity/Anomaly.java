package ru.homyakin.seeker.game.event.anomaly.entity;

import java.util.Optional;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.personage.models.PersonageId;

public record Anomaly(
    long launchedEventId,
    GroupId groupId,
    Optional<PersonageId> ownerPersonageId,
    AnomalyPhase phase,
    Optional<AnomalyMode> mode,
    String modifierCode,
    boolean rosterLocked,
    Optional<Long> opponentLaunchedEventId,
    Optional<Integer> gvgRatingAtStart,
    boolean isChallenge
) {
    public Anomaly withMode(AnomalyMode newMode) {
        return new Anomaly(
            launchedEventId,
            groupId,
            ownerPersonageId,
            AnomalyPhase.GATHERING,
            Optional.of(newMode),
            modifierCode,
            rosterLocked,
            opponentLaunchedEventId,
            gvgRatingAtStart,
            isChallenge
        );
    }

    public Anomaly withPhase(AnomalyPhase newPhase) {
        return new Anomaly(
            launchedEventId,
            groupId,
            ownerPersonageId,
            newPhase,
            mode,
            modifierCode,
            rosterLocked,
            opponentLaunchedEventId,
            gvgRatingAtStart,
            isChallenge
        );
    }

    public Anomaly withOwner(PersonageId personageId) {
        return new Anomaly(
            launchedEventId,
            groupId,
            Optional.of(personageId),
            phase,
            mode,
            modifierCode,
            rosterLocked,
            opponentLaunchedEventId,
            gvgRatingAtStart,
            isChallenge
        );
    }

    public Anomaly lockRoster() {
        return new Anomaly(
            launchedEventId,
            groupId,
            ownerPersonageId,
            phase,
            mode,
            modifierCode,
            true,
            opponentLaunchedEventId,
            gvgRatingAtStart,
            isChallenge
        );
    }

    public Anomaly withOpponent(Long opponentId) {
        return new Anomaly(
            launchedEventId,
            groupId,
            ownerPersonageId,
            phase,
            mode,
            modifierCode,
            rosterLocked,
            Optional.ofNullable(opponentId),
            gvgRatingAtStart,
            isChallenge
        );
    }

    public Anomaly clearOpponent() {
        return withOpponent(null);
    }

    public Anomaly withGvgRating(int rating) {
        return new Anomaly(
            launchedEventId,
            groupId,
            ownerPersonageId,
            phase,
            mode,
            modifierCode,
            rosterLocked,
            opponentLaunchedEventId,
            Optional.of(rating),
            isChallenge
        );
    }

    public boolean isOwner(PersonageId personageId) {
        return ownerPersonageId.filter(personageId::equals).isPresent();
    }
}
