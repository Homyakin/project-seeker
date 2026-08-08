package ru.homyakin.seeker.game.event.anomaly.action;

import java.time.Duration;
import java.util.Comparator;
import java.util.HashSet;
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
import ru.homyakin.seeker.game.event.anomaly.entity.NotifyAnomalyBattleFinished;
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
    private final AnomalyBattleService anomalyBattleService;
    private final NotifyAnomalyBattleFinished notifyAnomalyBattleFinished;
    private final LockService lockService;

    public AnomalyMatchmaker(
        AnomalyStorage anomalyStorage,
        AnomalyGvgStorage gvgStorage,
        AnomalyConfig config,
        LaunchedEventService launchedEventService,
        AnomalyBattleService anomalyBattleService,
        NotifyAnomalyBattleFinished notifyAnomalyBattleFinished,
        LockService lockService
    ) {
        this.anomalyStorage = anomalyStorage;
        this.gvgStorage = gvgStorage;
        this.config = config;
        this.launchedEventService = launchedEventService;
        this.anomalyBattleService = anomalyBattleService;
        this.notifyAnomalyBattleFinished = notifyAnomalyBattleFinished;
        this.lockService = lockService;
    }

    public void matchSearchingExpeditions() {
        final var matched = new HashSet<Long>();
        for (final var searching : anomalyStorage.findActiveSearchingWithoutOpponent()) {
            if (matched.contains(searching.id())) {
                continue;
            }
            final var key = LockPrefixes.LAUNCHED_EVENT.name() + searching.id();
            lockService.tryLockAndExecute(
                key,
                () -> tryMatchAsHost(searching.id()).ifPresent(guestId -> {
                    matched.add(searching.id());
                    matched.add(guestId);
                })
            );
        }
    }

    private Optional<Long> tryMatchAsHost(long hostEventId) {
        final var hostEventOpt = launchedEventService.getById(hostEventId);
        if (hostEventOpt.isEmpty() || hostEventOpt.get().isInFinalStatus()) {
            return Optional.empty();
        }
        final var hostEvent = hostEventOpt.get();
        final var hostAnomalyOpt = anomalyStorage.findByLaunchedEventId(hostEventId);
        if (!(hostAnomalyOpt.orElse(null) instanceof Anomaly.Dangerous.Searching host)) {
            return Optional.empty();
        }

        final var now = TimeUtils.moscowTime();
        final var hostSearchAge = searchAge(host, now);
        if (hostSearchAge.compareTo(config.dangerousMinSearchDuration()) < 0) {
            return Optional.empty();
        }
        final var rankedGuests = anomalyStorage.findActiveSearchingWithoutOpponent().stream()
            .filter(event -> event.id() != hostEventId)
            .map(event -> scoreGuest(host, event, hostSearchAge, now))
            .flatMap(Optional::stream)
            .sorted(Comparator.comparingLong(ScoredGuest::score))
            .toList();

        if (rankedGuests.isEmpty()) {
            logger.debug("No anomaly pool partner for event {}", hostEventId);
            return Optional.empty();
        }

        for (final var guest : rankedGuests) {
            final var guestKey = LockPrefixes.LAUNCHED_EVENT.name() + guest.eventId();
            final var matchedGuest = lockService.tryLockAndCalc(
                guestKey,
                () -> tryMergeAndFight(host, hostEvent, guest.eventId(), now)
            );
            if (matchedGuest.getOrElse(Optional.empty()).isPresent()) {
                return matchedGuest.get();
            }
        }
        logger.debug("No reservable anomaly pool partner for event {}", hostEventId);
        return Optional.empty();
    }

    private Optional<Long> tryMergeAndFight(
        Anomaly.Dangerous.Searching host,
        LaunchedEvent hostEvent,
        long guestEventId,
        java.time.LocalDateTime now
    ) {
        final var guestEventOpt = launchedEventService.getById(guestEventId);
        if (guestEventOpt.isEmpty() || guestEventOpt.get().isInFinalStatus()) {
            return Optional.empty();
        }
        final var guestAnomalyOpt = anomalyStorage.findByLaunchedEventId(guestEventId);
        if (!(guestAnomalyOpt.orElse(null) instanceof Anomaly.Dangerous.Searching guest)) {
            return Optional.empty();
        }
        if (guest.groupId().equals(host.groupId())) {
            return Optional.empty();
        }

        final var hostSearchAge = searchAge(host, now);
        final var guestSearchAge = searchAge(guest, now);
        if (hostSearchAge.compareTo(config.dangerousMinSearchDuration()) < 0
            || guestSearchAge.compareTo(config.dangerousMinSearchDuration()) < 0
        ) {
            return Optional.empty();
        }
        final int allowedDelta = maxAllowedRatingDiff(hostSearchAge, guestSearchAge);
        final int distance = Math.abs(guest.gvgRatingAtStart() - host.gvgRatingAtStart());
        if (distance > allowedDelta) {
            return Optional.empty();
        }

        final var accepted = host.matchWith(
            guest.groupId(),
            guest.ownerPersonageId(),
            guest.launchedEventId()
        );
        if (!anomalyStorage.tryMergeSearchingInto(accepted, guestEventId)) {
            return Optional.empty();
        }

        final var battleResult = anomalyBattleService.fight(
            launchedEventService.getById(hostEvent.id()).orElse(hostEvent),
            accepted
        );
        notifyAnomalyBattleFinished.notify(battleResult);
        logger.info(
            "Anomaly pool matched host {} (group {}) with guest {} (group {})",
            host.launchedEventId(),
            host.groupId().value(),
            guestEventId,
            guest.groupId().value()
        );
        return Optional.of(guestEventId);
    }

    private Optional<ScoredGuest> scoreGuest(
        Anomaly.Dangerous.Searching host,
        LaunchedEvent guestEvent,
        Duration hostSearchAge,
        java.time.LocalDateTime now
    ) {
        final var guestAnomalyOpt = anomalyStorage.findByLaunchedEventId(guestEvent.id());
        if (!(guestAnomalyOpt.orElse(null) instanceof Anomaly.Dangerous.Searching guest)) {
            return Optional.empty();
        }
        if (guest.groupId().equals(host.groupId())) {
            return Optional.empty();
        }
        final var guestSearchAge = searchAge(guest, now);
        if (guestSearchAge.compareTo(config.dangerousMinSearchDuration()) < 0) {
            return Optional.empty();
        }
        final int allowedDelta = maxAllowedRatingDiff(hostSearchAge, guestSearchAge);
        final int distance = Math.abs(guest.gvgRatingAtStart() - host.gvgRatingAtStart());
        if (distance > allowedDelta) {
            return Optional.empty();
        }
        final var recentPenalties = AnomalyGvgMatchRules.sumRecentMeetPenalties(
            gvgStorage.findOpponentFoughtAtList(host.groupId(), guest.groupId()),
            now,
            config.recentMeetPenaltyFirstDay()
        );
        return Optional.of(new ScoredGuest(
            guestEvent.id(),
            guest.groupId(),
            AnomalyGvgMatchRules.score(distance, recentPenalties)
        ));
    }

    private int maxAllowedRatingDiff(Duration hostSearchAge, Duration guestSearchAge) {
        final var searchAge = hostSearchAge.compareTo(guestSearchAge) >= 0
            ? hostSearchAge
            : guestSearchAge;
        return AnomalyGvgMatchRules.maxAllowedRatingDiff(
            searchAge,
            config.dangerousMinSearchDuration(),
            config.dangerousSearchDuration()
        );
    }

    private Duration searchAge(Anomaly.Dangerous.Searching searching, java.time.LocalDateTime now) {
        final var searchStartedAt = searching.searchEndDate().minus(config.dangerousSearchDuration());
        return Duration.between(searchStartedAt, now);
    }

    private record ScoredGuest(long eventId, GroupId groupId, long score) {
    }
}
