package ru.homyakin.seeker.game.event.anomaly.entity;

import java.util.Optional;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.personage.models.PersonageId;

public sealed interface Anomaly permits
    Anomaly.Safe,
    Anomaly.Dangerous,
    Anomaly.Challenged {

    long launchedEventId();

    GroupId groupId();

    Optional<PersonageId> ownerPersonageId();

    boolean rosterLocked();

    default boolean isOwner(PersonageId personageId) {
        return ownerPersonageId().filter(personageId::equals).isPresent();
    }

    default boolean isChallenge() {
        return this instanceof Challenged;
    }

    record Safe(
        long launchedEventId,
        GroupId groupId,
        Optional<PersonageId> ownerPersonageId,
        AnomalyPveTemplate template,
        Phase phase,
        boolean rosterLocked
    ) implements Anomaly {
        public enum Phase {
            GATHERING,
            PVE_WAITING,
        }

        public Safe startPveWaiting() {
            return new Safe(
                launchedEventId,
                groupId,
                ownerPersonageId,
                template,
                Phase.PVE_WAITING,
                true
            );
        }
    }

    record Dangerous(
        long launchedEventId,
        GroupId groupId,
        Optional<PersonageId> ownerPersonageId,
        Phase phase,
        boolean rosterLocked,
        Optional<Long> opponentLaunchedEventId,
        Optional<Integer> gvgRatingAtStart
    ) implements Anomaly {
        public enum Phase {
            GATHERING,
            SEARCHING,
        }

        public Dangerous startSearching(int gvgRating) {
            return new Dangerous(
                launchedEventId,
                groupId,
                ownerPersonageId,
                Phase.SEARCHING,
                true,
                opponentLaunchedEventId,
                Optional.of(gvgRating)
            );
        }

        public Dangerous withOpponent(long opponentId) {
            return new Dangerous(
                launchedEventId,
                groupId,
                ownerPersonageId,
                phase,
                rosterLocked,
                Optional.of(opponentId),
                gvgRatingAtStart
            );
        }

        public Dangerous clearOpponent() {
            return new Dangerous(
                launchedEventId,
                groupId,
                ownerPersonageId,
                phase,
                rosterLocked,
                Optional.empty(),
                gvgRatingAtStart
            );
        }
    }

    record Challenged(
        long launchedEventId,
        GroupId groupId,
        Optional<PersonageId> ownerPersonageId,
        long initiatorLaunchedEventId,
        boolean rosterLocked
    ) implements Anomaly {
        public Challenged withOwner(PersonageId personageId) {
            return new Challenged(
                launchedEventId,
                groupId,
                Optional.of(personageId),
                initiatorLaunchedEventId,
                rosterLocked
            );
        }

        public Challenged lockRoster() {
            return new Challenged(
                launchedEventId,
                groupId,
                ownerPersonageId,
                initiatorLaunchedEventId,
                true
            );
        }
    }
}
