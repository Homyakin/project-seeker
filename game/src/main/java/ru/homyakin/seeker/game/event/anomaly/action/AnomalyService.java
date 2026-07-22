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
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyModifier;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyPhase;
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
    public Either<AnomalyError, LaunchedEvent> start(GroupId groupId, PersonageId personageId) {
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
        final var modifier = AnomalyModifier.random();
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
        anomalyStorage.save(new Anomaly(
            launched.id(),
            groupId,
            Optional.of(personageId),
            AnomalyPhase.CHOOSING_MODE,
            Optional.empty(),
            modifier.code(),
            false,
            Optional.empty(),
            Optional.empty(),
            false
        ));
        return Either.right(launchedEventService.getById(launched.id()).orElse(launched));
    }

    public Either<AnomalyError, LaunchedEvent> chooseMode(
        long launchedEventId,
        PersonageId personageId,
        AnomalyMode mode
    ) {
        return withLock(launchedEventId, () -> chooseModeLogic(launchedEventId, personageId, mode));
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
            return EventResult.AnomalyResult.ExpiredChoosingOrGathering.INSTANCE;
        }
        final var anomaly = anomalyOpt.get();
        return switch (anomaly.phase()) {
            case CHOOSING_MODE, GATHERING, CHALLENGED -> {
                if (anomaly.phase() == AnomalyPhase.CHALLENGED) {
                    anomaly.opponentLaunchedEventId().ifPresent(initiatorId ->
                        clearOpponentLink(initiatorId, launchedEvent.id())
                    );
                }
                launchedEventService.updateStatus(launchedEvent.id(), EventStatus.EXPIRED);
                yield EventResult.AnomalyResult.ExpiredChoosingOrGathering.INSTANCE;
            }
            case SEARCHING -> {
                anomaly.opponentLaunchedEventId().ifPresent(launchedEventService::cancel);
                payParticipants(launchedEvent.id(), config.noMatchReward());
                launchedEventService.updateStatus(launchedEvent.id(), EventStatus.SUCCESS);
                yield new EventResult.AnomalyResult.NoMatch(launchedEvent.id());
            }
        };
    }

    public Optional<Anomaly> findAnomaly(long launchedEventId) {
        return anomalyStorage.findByLaunchedEventId(launchedEventId);
    }

    public ListParticipants participants(long launchedEventId) {
        return new ListParticipants(personageEventService.getParticipants(launchedEventId));
    }

    private Either<AnomalyError, LaunchedEvent> chooseModeLogic(
        long launchedEventId,
        PersonageId personageId,
        AnomalyMode mode
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
        if (anomaly.phase() != AnomalyPhase.CHOOSING_MODE) {
            return Either.left(AnomalyError.InvalidPhase.INSTANCE);
        }
        if (!anomaly.isOwner(personageId)) {
            return Either.left(AnomalyError.NotOwner.INSTANCE);
        }
        anomalyStorage.update(anomaly.withMode(mode));
        return Either.right(event);
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
        if (anomaly.phase() != AnomalyPhase.GATHERING
            && anomaly.phase() != AnomalyPhase.CHALLENGED) {
            return Either.left(AnomalyError.InvalidPhase.INSTANCE);
        }
        if (anomaly.rosterLocked()) {
            return Either.left(AnomalyError.RosterLocked.INSTANCE);
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

        if (anomaly.phase() == AnomalyPhase.CHALLENGED && participants.isEmpty()) {
            anomalyStorage.update(anomaly.withOwner(personageId));
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

        return switch (anomaly.phase()) {
            case GATHERING -> readyGathering(event, anomaly, participants);
            case CHALLENGED -> readyChallenged(event, anomaly, participants);
            default -> Either.left(AnomalyError.InvalidPhase.INSTANCE);
        };
    }

    private Either<AnomalyError, AnomalyReadyResult> readyGathering(
        LaunchedEvent event,
        Anomaly anomaly,
        java.util.List<EventParticipant> participants
    ) {
        if (anomaly.mode().isEmpty()) {
            return Either.left(AnomalyError.InvalidPhase.INSTANCE);
        }
        if (anomaly.mode().get() == AnomalyMode.SAFE) {
            anomalyStorage.update(anomaly.lockRoster());
            payParticipants(event.id(), config.safeReward());
            launchedEventService.updateStatus(event.id(), EventStatus.SUCCESS);
            return Either.right(new AnomalyReadyResult.SafeCompleted(event.id()));
        }
        if (participants.size() < config.partySize()) {
            return Either.left(AnomalyError.PartyNotFull.INSTANCE);
        }
        final var now = TimeUtils.moscowTime();
        final var searching = anomaly
            .lockRoster()
            .withPhase(AnomalyPhase.SEARCHING)
            .withGvgRating(gvgStorage.getRating(anomaly.groupId()));
        anomalyStorage.update(searching);
        launchedEventService.updateEndDate(event.id(), now.plus(config.dangerousSearchDuration()));
        return Either.right(new AnomalyReadyResult.StartedSearching(
            launchedEventService.getById(event.id()).orElseThrow()
        ));
    }

    private Either<AnomalyError, AnomalyReadyResult> readyChallenged(
        LaunchedEvent challengedEvent,
        Anomaly challengedAnomaly,
        java.util.List<EventParticipant> participants
    ) {
        if (participants.size() < config.partySize()) {
            return Either.left(AnomalyError.PartyNotFull.INSTANCE);
        }
        final var initiatorId = challengedAnomaly.opponentLaunchedEventId()
            .orElseThrow(() -> new IllegalStateException("Challenged anomaly without initiator"));
        final var initiatorEvent = launchedEventService.getById(initiatorId)
            .orElseThrow(() -> new IllegalStateException("Initiator anomaly missing"));
        final var initiatorAnomaly = findAnomaly(initiatorEvent.id())
            .orElseThrow(() -> new IllegalStateException("Initiator anomaly row missing"));
        if (initiatorEvent.isInFinalStatus() || initiatorAnomaly.phase() != AnomalyPhase.SEARCHING) {
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

    private void clearOpponentLink(long initiatorLaunchedEventId, long challengedLaunchedEventId) {
        findAnomaly(initiatorLaunchedEventId).ifPresent(anomaly -> {
            if (anomaly.opponentLaunchedEventId().filter(id -> id == challengedLaunchedEventId).isPresent()) {
                anomalyStorage.update(anomaly.clearOpponent());
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
        record SafeCompleted(long launchedEventId) implements AnomalyReadyResult {
        }

        record StartedSearching(LaunchedEvent launchedEvent) implements AnomalyReadyResult {
        }

        record BattleCompleted(EventResult.AnomalyResult.BattleFinished result) implements AnomalyReadyResult {
        }
    }
}
