package ru.homyakin.seeker.game.event.anomaly.action;

import java.time.Duration;
import java.util.Comparator;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.event.anomaly.entity.Anomaly;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyConfig;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyGvgMatchRules;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyGvgStorage;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyStorage;
import ru.homyakin.seeker.game.event.anomaly.entity.SendAnomalyChallengeToGroup;
import ru.homyakin.seeker.game.event.launched.LaunchedEvent;
import ru.homyakin.seeker.game.event.launched.LaunchedEventService;
import ru.homyakin.seeker.infrastructure.lock.LockPrefixes;
import ru.homyakin.seeker.infrastructure.lock.LockService;
import ru.homyakin.seeker.utils.TimeUtils;

@Service
public class AnomalyMatchmaker {
    private static final Logger logger = LoggerFactory.getLogger(AnomalyMatchmaker.class);

    private final AnomalyStorage anomalyStorage;
    private final AnomalyGvgStorage gvgStorage;
    private final AnomalyConfig config;
    private final LaunchedEventService launchedEventService;
    private final SendAnomalyChallengeToGroup sendAnomalyChallengeToGroup;
    private final LockService lockService;

    public AnomalyMatchmaker(
        AnomalyStorage anomalyStorage,
        AnomalyGvgStorage gvgStorage,
        AnomalyConfig config,
        LaunchedEventService launchedEventService,
        SendAnomalyChallengeToGroup sendAnomalyChallengeToGroup,
        LockService lockService
    ) {
        this.anomalyStorage = anomalyStorage;
        this.gvgStorage = gvgStorage;
        this.config = config;
        this.launchedEventService = launchedEventService;
        this.sendAnomalyChallengeToGroup = sendAnomalyChallengeToGroup;
        this.lockService = lockService;
    }

    public void matchSearchingExpeditions() {
        for (final var searching : anomalyStorage.findActiveSearchingWithoutOpponent()) {
            final var key = LockPrefixes.LAUNCHED_EVENT.name() + searching.id();
            lockService.tryLockAndExecute(key, () -> tryInvite(searching.id()));
        }
    }

    private void tryInvite(long searchingEventId) {
        final var eventOpt = launchedEventService.getById(searchingEventId);
        if (eventOpt.isEmpty() || eventOpt.get().isInFinalStatus()) {
            return;
        }
        final var searchingEvent = eventOpt.get();
        final var anomalyOpt = anomalyStorage.findByLaunchedEventId(searchingEventId);
        if (!(anomalyOpt.orElse(null) instanceof Anomaly.Dangerous.Searching searching)) {
            return;
        }

        final var initiatorGroupId = searching.groupId();
        final var rating = searching.gvgRatingAtStart();
        final var now = TimeUtils.moscowTime();
        final var searchStartedAt = searching.searchEndDate().minus(config.dangerousSearchDuration());
        final var searchAge = Duration.between(searchStartedAt, now);
        final int allowedDelta = AnomalyGvgMatchRules.maxAllowedRatingDiff(searchAge);

        final var rankedTargets = gvgStorage.findEligibleChallengeTargets(initiatorGroupId).stream()
            .map(targetId -> scoreTarget(initiatorGroupId, targetId, rating, allowedDelta, now))
            .flatMap(Optional::stream)
            .sorted(Comparator.comparingLong(ScoredTarget::score))
            .toList();

        if (rankedTargets.isEmpty()) {
            logger.debug("No anomaly challenge target for event {}", searchingEventId);
            return;
        }

        for (final var target : rankedTargets) {
            final var defenderKey = LockPrefixes.ANOMALY_GROUP.name() + target.groupId().value();
            final var reserved = lockService.tryLockAndCalc(
                defenderKey,
                () -> tryReserveOpponent(searching, searchingEvent, target.groupId(), now)
            );
            if (reserved.getOrElse(false)) {
                return;
            }
        }
        logger.debug("No reservable anomaly challenge target for event {}", searchingEventId);
    }

    private boolean tryReserveOpponent(
        Anomaly.Dangerous.Searching searching,
        LaunchedEvent searchingEvent,
        GroupId targetGroupId,
        java.time.LocalDateTime now
    ) {
        if (anomalyStorage.hasActiveAnomaly(targetGroupId)) {
            return false;
        }
        final var challenged = searching.withOpponent(targetGroupId);
        if (!anomalyStorage.tryAssignOpponent(challenged)) {
            return false;
        }

        launchedEventService.addGroupToEvent(searchingEvent.id(), targetGroupId);
        final var challengeEnd = now.plus(config.dangerousChallengeDuration());
        launchedEventService.updateEndDate(searchingEvent.id(), challengeEnd);

        sendAnomalyChallengeToGroup.send(
            targetGroupId,
            launchedEventService.getById(searchingEvent.id()).orElse(searchingEvent)
        );
        logger.info(
            "Anomaly {} invited group {}",
            searchingEvent.id(),
            targetGroupId.value()
        );
        return true;
    }

    private Optional<ScoredTarget> scoreTarget(
        GroupId initiatorGroupId,
        GroupId targetId,
        int initiatorRating,
        int allowedDelta,
        java.time.LocalDateTime now
    ) {
        final var targetRating = gvgStorage.getRating(targetId);
        final int distance = Math.abs(targetRating - initiatorRating);
        if (distance > allowedDelta) {
            return Optional.empty();
        }
        final var recentPenalties = AnomalyGvgMatchRules.sumRecentMeetPenalties(
            gvgStorage.findOpponentFoughtAtList(initiatorGroupId, targetId),
            now,
            config.recentMeetPenaltyFirstDay()
        );
        return Optional.of(new ScoredTarget(
            targetId,
            AnomalyGvgMatchRules.score(distance, recentPenalties)
        ));
    }

    private record ScoredTarget(GroupId groupId, long score) {
    }
}
