package ru.homyakin.seeker.game.event.anomaly.action;

import io.vavr.control.Either;
import java.util.List;
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
        if (launchedEventService.getActiveEventsByPersonageId(personageId).hasType(EventType.ANOMALY)) {
            return Either.left(AnomalyError.AlreadyInAnomaly.INSTANCE);
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
                personageId,
                AnomalyPveTemplate.random(),
                Anomaly.Safe.Phase.GATHERING
            );
            case DANGEROUS -> new Anomaly.Dangerous.Gathering(
                launched.id(),
                groupId,
                personageId
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
            case Anomaly.Dangerous.Accepted accepted when accepted.winnerGroupId().isPresent() ->
                EventResult.AnomalyResult.AlreadyFinal.INSTANCE;
            case Anomaly.Dangerous.Accepted accepted ->
                anomalyBattleService.fight(launchedEvent, accepted);
            case Anomaly.Dangerous.Searching searching ->
                anomalyBattleService.fightPveFallback(launchedEvent, searching.groupId());
            case Anomaly.Safe _, Anomaly.Dangerous.Gathering _ -> {
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
        final var personage = personageService.getByIdForce(personageId);

        final GroupId joinGroupId;
        final boolean canJoin;
        switch (anomaly) {
            case Anomaly.Safe safe -> {
                joinGroupId = safe.groupId();
                canJoin = safe.phase() == Anomaly.Safe.Phase.GATHERING;
            }
            case Anomaly.Dangerous.Gathering gathering -> {
                joinGroupId = gathering.groupId();
                canJoin = true;
            }
            case Anomaly.Dangerous.Searching _, Anomaly.Dangerous.Accepted _ -> {
                joinGroupId = anomaly.groupId();
                canJoin = false;
            }
        }

        if (!canJoin) {
            return Either.left(
                switch (anomaly) {
                    case Anomaly.Safe safe when safe.phase() == Anomaly.Safe.Phase.PVE_WAITING ->
                        AnomalyError.RosterLocked.INSTANCE;
                    case Anomaly.Dangerous.Searching _ -> AnomalyError.RosterLocked.INSTANCE;
                    default -> AnomalyError.InvalidPhase.INSTANCE;
                }
            );
        }
        if (!isMember(personage, joinGroupId)) {
            return Either.left(AnomalyError.NotGroupMember.INSTANCE);
        }
        if (launchedEventService.getActiveEventsByPersonageId(personageId)
            .hasOtherOfType(EventType.ANOMALY, event.id())
        ) {
            return Either.left(AnomalyError.AlreadyInAnomaly.INSTANCE);
        }

        final var sideParticipants = participantsOfGroup(
            personageEventService.getParticipants(event.id()),
            joinGroupId
        );
        if (sideParticipants.stream().anyMatch(it -> it.personage().id().equals(personageId))) {
            return Either.left(AnomalyError.AlreadyJoined.INSTANCE);
        }
        if (sideParticipants.size() >= config.partySize()) {
            return Either.left(AnomalyError.PartyFull.INSTANCE);
        }

        // Already under LAUNCHED_EVENT lock (non-reentrant); do not call addPersonageToLaunchedEvent.
        personageEventService.addPersonageToLaunchedEventAssumingLocked(
            new AddPersonageToEventRequest(event.id(), personageId, Optional.empty(), 0)
        );
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

        return switch (anomaly) {
            case Anomaly.Safe safe when safe.phase() == Anomaly.Safe.Phase.GATHERING -> {
                if (!safe.isOwner(personageId)) {
                    yield Either.left(AnomalyError.NotOwner.INSTANCE);
                }
                final var participants = personageEventService.getParticipants(event.id());
                if (participants.isEmpty()) {
                    yield Either.left(AnomalyError.PartyEmpty.INSTANCE);
                }
                yield readySafe(event, safe);
            }
            case Anomaly.Dangerous.Gathering gathering -> {
                if (!gathering.isOwner(personageId)) {
                    yield Either.left(AnomalyError.NotOwner.INSTANCE);
                }
                final var participants = participantsOfGroup(
                    personageEventService.getParticipants(event.id()),
                    gathering.groupId()
                );
                if (participants.isEmpty()) {
                    yield Either.left(AnomalyError.PartyEmpty.INSTANCE);
                }
                yield readyDangerous(event, gathering, participants);
            }
            case Anomaly.Safe _, Anomaly.Dangerous.Searching _, Anomaly.Dangerous.Accepted _ ->
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
        Anomaly.Dangerous.Gathering gathering,
        List<EventParticipant> participants
    ) {
        if (participants.size() < config.partySize()) {
            return Either.left(AnomalyError.PartyNotFull.INSTANCE);
        }
        final var searchEnd = TimeUtils.moscowTime().plus(config.dangerousSearchDuration());
        anomalyStorage.update(
            gathering.startSearching(gvgStorage.getRating(gathering.groupId()))
        );
        launchedEventService.updateEndDate(event.id(), searchEnd);
        return Either.right(new AnomalyReadyResult.StartedSearching(
            launchedEventService.getById(event.id()).orElseThrow()
        ));
    }

    private static List<EventParticipant> participantsOfGroup(
        List<EventParticipant> participants,
        GroupId groupId
    ) {
        return participants.stream()
            .filter(participant -> participant.personage().memberGroupId()
                .filter(groupId::equals)
                .isPresent())
            .toList();
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

    public record ListParticipants(List<EventParticipant> list) {
    }

    public sealed interface AnomalyReadyResult {
        record StartedPveWaiting(LaunchedEvent launchedEvent) implements AnomalyReadyResult {
        }

        record StartedSearching(LaunchedEvent launchedEvent) implements AnomalyReadyResult {
        }
    }
}
