package ru.homyakin.seeker.game.event.anomaly.entity;

import java.time.LocalDateTime;
import java.util.Optional;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.personage.models.PersonageId;

public sealed interface Anomaly permits
    Anomaly.Safe,
    Anomaly.Dangerous {

    long launchedEventId();

    GroupId groupId();

    PersonageId ownerPersonageId();

    default boolean isOwner(PersonageId personageId) {
        return ownerPersonageId().equals(personageId);
    }

    record Safe(
        long launchedEventId,
        GroupId groupId,
        PersonageId ownerPersonageId,
        AnomalyPveTemplate template,
        Phase phase
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
                Phase.PVE_WAITING
            );
        }
    }

    sealed interface Dangerous extends Anomaly permits
        Dangerous.Gathering,
        Dangerous.Searching,
        Dangerous.Accepted {

        record Gathering(
            long launchedEventId,
            GroupId groupId,
            PersonageId ownerPersonageId
        ) implements Dangerous {
            public Searching startSearching(int gvgRatingAtStart, LocalDateTime searchEndDate) {
                return new Searching(
                    launchedEventId,
                    groupId,
                    ownerPersonageId,
                    gvgRatingAtStart,
                    searchEndDate
                );
            }
        }

        record Searching(
            long launchedEventId,
            GroupId groupId,
            PersonageId ownerPersonageId,
            int gvgRatingAtStart,
            LocalDateTime searchEndDate
        ) implements Dangerous {
            /**
             * Pool match: both sides already gathered, go straight to Accepted
             * with a link to the opponent anomaly.
             */
            public Accepted matchWith(
                GroupId opponentGroupId,
                PersonageId opponentOwnerPersonageId,
                long opponentLaunchedEventId
            ) {
                return new Accepted(
                    launchedEventId,
                    groupId,
                    ownerPersonageId,
                    opponentGroupId,
                    opponentOwnerPersonageId,
                    Optional.of(opponentLaunchedEventId),
                    Optional.empty(),
                    gvgRatingAtStart,
                    searchEndDate
                );
            }
        }

        /**
         * Pool match completed; battle starts immediately via matchmaker.
         * {@code opponentLaunchedEventId} links to the other group's anomaly.
         */
        record Accepted(
            long launchedEventId,
            GroupId groupId,
            PersonageId ownerPersonageId,
            GroupId opponentGroupId,
            PersonageId opponentOwnerPersonageId,
            Optional<Long> opponentLaunchedEventId,
            Optional<GroupId> winnerGroupId,
            int gvgRatingAtStart,
            LocalDateTime searchEndDate
        ) implements Dangerous {
            public Accepted withWinner(GroupId winnerGroupId) {
                return new Accepted(
                    launchedEventId,
                    groupId,
                    ownerPersonageId,
                    opponentGroupId,
                    opponentOwnerPersonageId,
                    opponentLaunchedEventId,
                    Optional.of(winnerGroupId),
                    gvgRatingAtStart,
                    searchEndDate
                );
            }
        }
    }
}
