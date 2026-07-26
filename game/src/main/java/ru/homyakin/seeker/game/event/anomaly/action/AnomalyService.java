package ru.homyakin.seeker.game.event.anomaly.action;

import io.vavr.control.Either;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.event.anomaly.entity.Anomaly;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyConfig;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyError;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyGvgStorage;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyMode;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyPveTemplate;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyStorage;
import ru.homyakin.seeker.game.event.launched.LaunchedEvent;
import ru.homyakin.seeker.game.event.launched.LaunchedEventService;
import ru.homyakin.seeker.game.event.models.EventResult;
import ru.homyakin.seeker.game.event.models.EventStatus;
import ru.homyakin.seeker.game.event.models.EventType;
import ru.homyakin.seeker.game.event.service.EventService;
import ru.homyakin.seeker.game.group.action.GetGroup;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.outpost.entity.Building;
import ru.homyakin.seeker.game.outpost.entity.OutpostStorage;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.event.AddPersonageToEventRequest;
import ru.homyakin.seeker.game.personage.event.EventParticipant;
import ru.homyakin.seeker.game.personage.event.PersonageEventService;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.infrastructure.lock.LockPrefixes;
import ru.homyakin.seeker.infrastructure.lock.LockService;
import ru.homyakin.seeker.utils.TimeUtils;

@Service
public class AnomalyService {
    private final GetGroup getGroup;
    private final OutpostStorage outpostStorage;
    private final AnomalyStorage anomalyStorage;
    private final AnomalyGvgStorage gvgStorage;
    private final AnomalyConfig config;
    private final EventService eventService;
    private final LaunchedEventService launchedEventService;
    private final PersonageService personageService;
    private final PersonageEventService personageEventService;
    private final AnomalyBattleService anomalyBattleService;
    private final LockService lockService;

    public AnomalyService(
        GetGroup getGroup,
        OutpostStorage outpostStorage,
        AnomalyStorage anomalyStorage,
        AnomalyGvgStorage gvgStorage,
        AnomalyConfig config,
        EventService eventService,
        LaunchedEventService launchedEventService,
        PersonageService personageService,
        PersonageEventService personageEventService,
        AnomalyBattleService anomalyBattleService,
        LockService lockService
    ) {
        this.getGroup = getGroup;
        this.outpostStorage = outpostStorage;
        this.anomalyStorage = anomalyStorage;
        this.gvgStorage = gvgStorage;
        this.config = config;
        this.eventService = eventService;
        this.launchedEventService = launchedEventService;
        this.personageService = personageService;
        this.personageEventService = personageEventService;
        this.anomalyBattleService = anomalyBattleService;
        this.lockService = lockService;
    }

    public boolean isEligibleForMenu(GroupId groupId) {
        return hasStormScanner(groupId);
    }

    public boolean canStartToday(GroupId groupId) {
        return !anomalyStorage.hasStartOnDate(groupId, TimeUtils.moscowDate())
            && !anomalyStorage.hasActiveAnomaly(groupId);
    }

    public Optional<LaunchedEvent> findActive(GroupId groupId) {
        return anomalyStorage.findActiveLaunchedEventByGroupId(groupId);
    }

    @Transactional
    public Either<AnomalyError, LaunchedEvent> start(
        GroupId groupId,
        PersonageId personageId,
        AnomalyMode mode
    ) {
        final var group = getGroup.forceGet(groupId);
        if (!group.isRegistered()) {
            return Either.left(AnomalyError.NotRegistered.INSTANCE);
        }
        if (!hasStormScanner(groupId)) {
            return Either.left(AnomalyError.NoStormScanner.INSTANCE);
        }
        final var personage = personageService.getByIdForce(personageId);
        if (!isMember(personage, groupId)) {
            return Either.left(AnomalyError.NotGroupMember.INSTANCE);
        }
        if (anomalyStorage.hasStartOnDate(groupId, TimeUtils.moscowDate())) {
            return Either.left(AnomalyError.AlreadyStartedToday.INSTANCE);
        }
        if (anomalyStorage.hasActiveAnomaly(groupId)) {
            return Either.left(AnomalyError.ActiveAnomalyExists.INSTANCE);
        }

        final var event = eventService.getByType(EventType.ANOMALY)
            .orElseThrow(() -> new IllegalStateException("Anomaly event template missing"));
        final var now = TimeUtils.moscowTime();
        final var launched = launchedEventService.createFromAnomaly(
            event.id(),
            now,
            now.plus(config.gatheringDuration()),
            groupId
        );
        final var joinResult = personageEventService.addPersonageToLaunchedEvent(
            new AddPersonageToEventRequest(launched.id(), personageId, Optional.empty(), 0)
        );
        if (joinResult.isLeft()) {
            launchedEventService.cancel(launched.id());
            return Either.left(AnomalyError.EventLocked.INSTANCE);
        }
        final Anomaly anomaly = switch (mode) {
            case SAFE -> new Anomaly.Safe(
                launched.id(),
                groupId,
                Optional.of(personageId),
                AnomalyPveTemplate.random(),
                Anomaly.Safe.Phase.GATHERING,
                false
            );
            case DANGEROUS -> new Anomaly.Dangerous(
                launched.id(),
                groupId,
                Optional.of(personageId),
                Anomaly.Dangerous.Phase.GATHERING,
                false,
                Optional.empty(),
                Optional.empty()
            );
        };
        anomalyStorage.save(anomaly);
        return Either.right(launchedEventService.getById(launched.id()).orElse(launched));
    }

    public Either<AnomalyError, LaunchedEvent> join(long launchedEventId, PersonageId personageId) {
        return withLock(launchedEventId, () -> joinLogic(launchedEventId, personageId));
    }

    public Either<AnomalyError, AnomalyReadyResult> ready(long launchedEventId, PersonageId personageId) {
        return withLock(launchedEventId, () -> readyLogic(launchedEventId, personageId));
    }

    public EventResult processExpired(LaunchedEvent launchedEvent) {
        if (launchedEvent.isInFinalStatus()) {
            return EventResult.AnomalyResult.AlreadyFinal.INSTANCE;
        }
        final var anomalyOpt = findAnomaly(launchedEvent.id());
        if (anomalyOpt.isEmpty()) {
            launchedEventService.updateStatus(launchedEvent.id(), EventStatus.EXPIRED);
            return EventResult.AnomalyResult.ExpiredGathering.INSTANCE;
        }
        return switch (anomalyOpt.get()) {
            case Anomaly.Safe safe when safe.phase() == Anomaly.Safe.Phase.PVE_WAITING ->
                anomalyBattleService.fightPve(launchedEvent, safe);
            case Anomaly.Dangerous dangerous when dangerous.phase() == Anomaly.Dangerous.Phase.SEARCHING -> {
                dangerous.opponentLaunchedEventId().ifPresent(launchedEventService::cancel);
                payParticipants(launchedEvent.id(), config.noMatchReward());
                launchedEventService.updateStatus(launchedEvent.id(), EventStatus.SUCCESS);
                yield new EventResult.AnomalyResult.NoMatch(launchedEvent.id());
            }
            case Anomaly.Challenged challenged -> {
                clearOpponentLinkOnChallengedExpire(challenged);
                launchedEventService.updateStatus(launchedEvent.id(), EventStatus.EXPIRED);
                yield EventResult.AnomalyResult.ExpiredGathering.INSTANCE;
            }
            case Anomaly.Safe _, Anomaly.Dangerous _ -> {
                launchedEventService.updateStatus(launchedEvent.id(), EventStatus.EXPIRED);
                yield EventResult.AnomalyResult.ExpiredGathering.INSTANCE;
            }
        };
    }

    public Optional<Anomaly> findAnomaly(long launchedEventId) {
        return anomalyStorage.findByLaunchedEventId(launchedEventId);
    }

    public ListParticipants participants(long launchedEventId) {
        return new ListParticipants(personageEventService.getParticipants(launchedEventId));
    }

    private Either<AnomalyError, LaunchedEvent> joinLogic(long launchedEventId, PersonageId personageId) {
        final var launched = requireEvent(launchedEventId);
        if (launched.isLeft()) {
            return Either.left(launched.getLeft());
        }
        final var event = launched.get();
        final var anomalyResult = requireAnomaly(event.id());
        if (anomalyResult.isLeft()) {
            return Either.left(anomalyResult.getLeft());
        }
        final var anomaly = anomalyResult.get();
        final boolean canJoin = switch (anomaly) {
            case Anomaly.Safe safe ->
                safe.phase() == Anomaly.Safe.Phase.GATHERING && !safe.rosterLocked();
            case Anomaly.Dangerous dangerous ->
                dangerous.phase() == Anomaly.Dangerous.Phase.GATHERING && !dangerous.rosterLocked();
            case Anomaly.Challenged challenged -> !challenged.rosterLocked();
        };
        if (!canJoin) {
            if (anomaly.rosterLocked()) {
                return Either.left(AnomalyError.RosterLocked.INSTANCE);
            }
            return Either.left(AnomalyError.InvalidPhase.INSTANCE);
        }
        final var personage = personageService.getByIdForce(personageId);
        if (!isMember(personage, anomaly.groupId())) {
            return Either.left(AnomalyError.NotGroupMember.INSTANCE);
        }
        final var participants = personageEventService.getParticipants(event.id());
        if (participants.stream().anyMatch(it -> it.personage().id().equals(personageId))) {
            return Either.left(AnomalyError.AlreadyJoined.INSTANCE);
        }
        if (participants.size() >= config.partySize()) {
            return Either.left(AnomalyError.PartyFull.INSTANCE);
        }

        if (anomaly instanceof Anomaly.Challenged challenged && participants.isEmpty()) {
            anomalyStorage.update(challenged.withOwner(personageId));
        }

        final var joinResult = personageEventService.addPersonageToLaunchedEvent(
            new AddPersonageToEventRequest(event.id(), personageId, Optional.empty(), 0)
        );
        if (joinResult.isLeft()) {
            return Either.left(AnomalyError.EventLocked.INSTANCE);
        }
        return Either.right(event);
    }

    private Either<AnomalyError, AnomalyReadyResult> readyLogic(
        long launchedEventId,
        PersonageId personageId
    ) {
        final var launched = requireEvent(launchedEventId);
        if (launched.isLeft()) {
            return Either.left(launched.getLeft());
        }
        final var event = launched.get();
        final var anomalyResult = requireAnomaly(event.id());
        if (anomalyResult.isLeft()) {
            return Either.left(anomalyResult.getLeft());
        }
        final var anomaly = anomalyResult.get();
        if (!anomaly.isOwner(personageId)) {
            return Either.left(AnomalyError.NotOwner.INSTANCE);
        }
        final var participants = personageEventService.getParticipants(event.id());
        if (participants.isEmpty()) {
            return Either.left(AnomalyError.PartyEmpty.INSTANCE);
        }

        return switch (anomaly) {
            case Anomaly.Safe safe when safe.phase() == Anomaly.Safe.Phase.GATHERING ->
                readySafe(event, safe);
            case Anomaly.Dangerous dangerous when dangerous.phase() == Anomaly.Dangerous.Phase.GATHERING ->
                readyDangerous(event, dangerous, participants);
            case Anomaly.Challenged challenged ->
                readyChallenged(event, challenged, participants);
            case Anomaly.Safe _, Anomaly.Dangerous _ ->
                Either.left(AnomalyError.InvalidPhase.INSTANCE);
        };
    }

    private Either<AnomalyError, AnomalyReadyResult> readySafe(
        LaunchedEvent event,
        Anomaly.Safe safe
    ) {
        anomalyStorage.update(safe.startPveWaiting());
        launchedEventService.updateEndDate(
            event.id(),
            TimeUtils.moscowTime().plus(config.safePveDuration())
        );
        return Either.right(new AnomalyReadyResult.StartedPveWaiting(
            launchedEventService.getById(event.id()).orElseThrow()
        ));
    }

    private Either<AnomalyError, AnomalyReadyResult> readyDangerous(
        LaunchedEvent event,
        Anomaly.Dangerous dangerous,
        java.util.List<EventParticipant> participants
    ) {
        if (participants.size() < config.partySize()) {
            return Either.left(AnomalyError.PartyNotFull.INSTANCE);
        }
        anomalyStorage.update(dangerous.startSearching(gvgStorage.getRating(dangerous.groupId())));
        launchedEventService.updateEndDate(
            event.id(),
            TimeUtils.moscowTime().plus(config.dangerousSearchDuration())
        );
        return Either.right(new AnomalyReadyResult.StartedSearching(
            launchedEventService.getById(event.id()).orElseThrow()
        ));
    }

    private Either<AnomalyError, AnomalyReadyResult> readyChallenged(
        LaunchedEvent challengedEvent,
        Anomaly.Challenged challengedAnomaly,
        java.util.List<EventParticipant> participants
    ) {
        if (participants.size() < config.partySize()) {
            return Either.left(AnomalyError.PartyNotFull.INSTANCE);
        }
        final var initiatorEvent = launchedEventService.getById(challengedAnomaly.initiatorLaunchedEventId())
            .orElseThrow(() -> new IllegalStateException("Initiator anomaly missing"));
        final var initiatorAnomaly = findAnomaly(initiatorEvent.id())
            .orElseThrow(() -> new IllegalStateException("Initiator anomaly row missing"));
        if (initiatorEvent.isInFinalStatus()
            || !(initiatorAnomaly instanceof Anomaly.Dangerous dangerous)
            || dangerous.phase() != Anomaly.Dangerous.Phase.SEARCHING) {
            launchedEventService.cancel(challengedEvent.id());
            return Either.left(AnomalyError.FinalStatus.INSTANCE);
        }

        anomalyStorage.update(challengedAnomaly.lockRoster());
        final var battleResult = anomalyBattleService.fight(initiatorEvent, challengedEvent);
        return Either.right(new AnomalyReadyResult.BattleCompleted(battleResult));
    }

    private void payParticipants(long launchedEventId, Money reward) {
        for (final var participant : personageEventService.getParticipants(launchedEventId)) {
            personageService.addMoney(participant.personage(), reward);
        }
    }

    private void clearOpponentLinkOnChallengedExpire(Anomaly.Challenged challenged) {
        findAnomaly(challenged.initiatorLaunchedEventId()).ifPresent(anomaly -> {
            if (anomaly instanceof Anomaly.Dangerous dangerous
                && dangerous.opponentLaunchedEventId()
                .filter(id -> id == challenged.launchedEventId()).isPresent()) {
                anomalyStorage.update(dangerous.clearOpponent());
            }
        });
    }

    private Either<AnomalyError, LaunchedEvent> requireEvent(long launchedEventId) {
        final var event = launchedEventService.getById(launchedEventId);
        if (event.isEmpty() || event.get().isInFinalStatus()) {
            return Either.left(event.isEmpty() ? AnomalyError.EventNotFound.INSTANCE : AnomalyError.FinalStatus.INSTANCE);
        }
        return Either.right(event.get());
    }

    private Either<AnomalyError, Anomaly> requireAnomaly(long launchedEventId) {
        return findAnomaly(launchedEventId)
            .<Either<AnomalyError, Anomaly>>map(Either::right)
            .orElseGet(() -> Either.left(AnomalyError.EventNotFound.INSTANCE));
    }

    private boolean hasStormScanner(GroupId groupId) {
        return outpostStorage.findBuildingSlot(groupId, Building.STORM_SCANNER)
            .map(slot -> slot.level() > 0)
            .orElse(false);
    }

    private boolean isMember(Personage personage, GroupId groupId) {
        return personage.memberGroupId().filter(groupId::equals).isPresent();
    }

    private <T> Either<AnomalyError, T> withLock(
        long launchedEventId,
        java.util.function.Supplier<Either<AnomalyError, T>> action
    ) {
        return lockService.tryLockAndCalc(
            LockPrefixes.LAUNCHED_EVENT.name() + launchedEventId,
            action
        ).fold(
            _ -> Either.left(AnomalyError.EventLocked.INSTANCE),
            either -> either
        );
    }

    public record ListParticipants(java.util.List<EventParticipant> list) {
    }

    public sealed interface AnomalyReadyResult {
        record StartedPveWaiting(LaunchedEvent launchedEvent) implements AnomalyReadyResult {
        }

        record StartedSearching(LaunchedEvent launchedEvent) implements AnomalyReadyResult {
        }

        record BattleCompleted(EventResult.AnomalyResult.BattleFinished result) implements AnomalyReadyResult {
        }
    }
}
