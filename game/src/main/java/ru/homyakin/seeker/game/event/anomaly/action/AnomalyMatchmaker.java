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
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyGvgStorage;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyPhase;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyStorage;
import ru.homyakin.seeker.game.event.anomaly.entity.SendAnomalyChallengeToGroup;
import ru.homyakin.seeker.game.event.launched.LaunchedEventService;
import ru.homyakin.seeker.game.event.models.EventType;
import ru.homyakin.seeker.game.event.service.EventService;
import ru.homyakin.seeker.infrastructure.lock.LockPrefixes;
import ru.homyakin.seeker.infrastructure.lock.LockService;
import ru.homyakin.seeker.utils.TimeUtils;

@Service
public class AnomalyMatchmaker {
    private static final Logger logger = LoggerFactory.getLogger(AnomalyMatchmaker.class);
    private static final long RECENT_OPPONENT_SCORE_PENALTY = 10_000L;

    private final AnomalyStorage anomalyStorage;
    private final AnomalyGvgStorage gvgStorage;
    private final AnomalyConfig config;
    private final EventService eventService;
    private final LaunchedEventService launchedEventService;
    private final SendAnomalyChallengeToGroup sendAnomalyChallengeToGroup;
    private final LockService lockService;

    public AnomalyMatchmaker(
        AnomalyStorage anomalyStorage,
        AnomalyGvgStorage gvgStorage,
        AnomalyConfig config,
        EventService eventService,
        LaunchedEventService launchedEventService,
        SendAnomalyChallengeToGroup sendAnomalyChallengeToGroup,
        LockService lockService
    ) {
        this.anomalyStorage = anomalyStorage;
        this.gvgStorage = gvgStorage;
        this.config = config;
        this.eventService = eventService;
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
        final var searching = eventOpt.get();
        final var anomalyOpt = anomalyStorage.findByLaunchedEventId(searchingEventId);
        if (anomalyOpt.isEmpty()) {
            return;
        }
        final var anomaly = anomalyOpt.get();
        if (anomaly.phase() != AnomalyPhase.SEARCHING || anomaly.opponentLaunchedEventId().isPresent()) {
            return;
        }

        final var initiatorGroupId = anomaly.groupId();
        final var rating = anomaly.gvgRatingAtStart()
            .orElseGet(() -> gvgStorage.getRating(initiatorGroupId));
        final var searchStartedAt = searching.endDate().minus(config.dangerousSearchDuration());
        final var hoursInSearch = Math.max(
            0,
            Duration.between(searchStartedAt, TimeUtils.moscowTime()).toHours()
        );
        final int allowedDelta = config.initialRatingDelta()
            + (int) (hoursInSearch * config.ratingDeltaExpandPerHour());
        final var now = TimeUtils.moscowTime();
        final var recentCutoff = now.minus(config.recentOpponentPenaltyHours());

        final var bestTarget = gvgStorage.findEligibleChallengeTargets(initiatorGroupId).stream()
            .map(targetId -> scoreTarget(initiatorGroupId, targetId, rating, allowedDelta, recentCutoff))
            .flatMap(Optional::stream)
            .min(Comparator.comparingLong(ScoredTarget::score))
            .map(ScoredTarget::groupId);

        if (bestTarget.isEmpty()) {
            logger.debug("No anomaly challenge target for event {}", searchingEventId);
            return;
        }

        final var targetGroupId = bestTarget.get();
        final var template = eventService.getByType(EventType.ANOMALY)
            .orElseThrow(() -> new IllegalStateException("Anomaly event template missing"));
        final var challenged = launchedEventService.createFromAnomaly(
            template.id(),
            now,
            searching.endDate(),
            targetGroupId
        );
        anomalyStorage.save(new Anomaly(
            challenged.id(),
            targetGroupId,
            Optional.empty(),
            AnomalyPhase.CHALLENGED,
            Optional.empty(),
            anomaly.modifierCode(),
            false,
            Optional.of(searching.id()),
            Optional.empty(),
            true
        ));
        anomalyStorage.update(anomaly.withOpponent(challenged.id()));
        sendAnomalyChallengeToGroup.send(
            targetGroupId,
            launchedEventService.getById(challenged.id()).orElse(challenged),
            searching
        );
        logger.info(
            "Anomaly {} invited group {} via event {}",
            searchingEventId,
            targetGroupId.value(),
            challenged.id()
        );
    }

    private Optional<ScoredTarget> scoreTarget(
        GroupId initiatorGroupId,
        GroupId targetId,
        int initiatorRating,
        int allowedDelta,
        java.time.LocalDateTime recentCutoff
    ) {
        final var targetRating = gvgStorage.getRating(targetId);
        final int distance = Math.abs(targetRating - initiatorRating);
        if (distance > allowedDelta) {
            return Optional.empty();
        }
        long score = distance;
        final var recentFoughtAt = gvgStorage.findRecentOpponentFoughtAt(initiatorGroupId, targetId);
        if (recentFoughtAt.filter(at -> at.isAfter(recentCutoff)).isPresent()) {
            score += RECENT_OPPONENT_SCORE_PENALTY;
        }
        return Optional.of(new ScoredTarget(targetId, score));
    }

    private record ScoredTarget(GroupId groupId, long score) {
    }
}
