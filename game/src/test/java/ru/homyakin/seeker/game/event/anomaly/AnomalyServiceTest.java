package ru.homyakin.seeker.game.event.anomaly;

import io.vavr.control.Either;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.badge.entity.BadgeView;
import ru.homyakin.seeker.game.event.anomaly.action.AnomalyBattleService;
import ru.homyakin.seeker.game.event.anomaly.action.AnomalyService;
import ru.homyakin.seeker.game.event.anomaly.entity.Anomaly;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyConfig;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyError;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyGvgStorage;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyMode;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyPveTemplate;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyReward;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyStorage;
import ru.homyakin.seeker.game.event.launched.CurrentEvent;
import ru.homyakin.seeker.game.event.launched.CurrentEvents;
import ru.homyakin.seeker.game.event.launched.LaunchedEvent;
import ru.homyakin.seeker.game.event.launched.LaunchedEventService;
import ru.homyakin.seeker.game.event.models.EventResult;
import ru.homyakin.seeker.game.event.models.EventStatus;
import ru.homyakin.seeker.game.event.models.EventType;
import ru.homyakin.seeker.game.event.service.EventService;
import ru.homyakin.seeker.game.group.action.GetGroup;
import ru.homyakin.seeker.game.group.entity.Group;
import ru.homyakin.seeker.game.group.entity.GroupSettings;
import ru.homyakin.seeker.game.outpost.entity.Building;
import ru.homyakin.seeker.game.outpost.entity.OutpostSlot;
import ru.homyakin.seeker.game.outpost.entity.OutpostStorage;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.event.EventParticipant;
import ru.homyakin.seeker.game.personage.event.PersonageEventService;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.infrastructure.lock.LockService;
import ru.homyakin.seeker.test_utils.PersonageUtils;
import ru.homyakin.seeker.utils.TimeUtils;

public class AnomalyServiceTest {
    private final GetGroup getGroup = Mockito.mock();
    private final OutpostStorage outpostStorage = Mockito.mock();
    private final AnomalyStorage anomalyStorage = Mockito.mock();
    private final AnomalyGvgStorage gvgStorage = Mockito.mock();
    private final AnomalyConfig config = Mockito.mock();
    private final EventService eventService = Mockito.mock();
    private final LaunchedEventService launchedEventService = Mockito.mock();
    private final PersonageService personageService = Mockito.mock();
    private final PersonageEventService personageEventService = Mockito.mock();
    private final AnomalyBattleService anomalyBattleService = Mockito.mock();
    private final LockService lockService = Mockito.mock();

    private AnomalyService service;
    private final GroupId groupId = GroupId.from(10L);
    private final PersonageId personageId = PersonageId.from(1L);
    private final PersonageId otherPersonageId = PersonageId.from(2L);

    @BeforeEach
    void init() {
        service = new AnomalyService(
            getGroup,
            outpostStorage,
            anomalyStorage,
            gvgStorage,
            config,
            eventService,
            launchedEventService,
            personageService,
            personageEventService,
            anomalyBattleService,
            lockService
        );
        Mockito.when(config.partySize()).thenReturn(5);
        Mockito.when(config.gatheringDuration()).thenReturn(Duration.ofHours(1));
        Mockito.when(config.safePveDuration()).thenReturn(Duration.ofHours(3));
        Mockito.when(config.dangerousMinSearchDuration()).thenReturn(Duration.ofHours(1));
        Mockito.when(config.dangerousSearchDuration()).thenReturn(Duration.ofHours(12));
        Mockito.when(lockService.tryLockAndCalc(Mockito.anyString(), Mockito.any()))
            .thenAnswer(invocation -> Either.right(
                invocation.getArgument(1, java.util.function.Supplier.class).get()
            ));
        Mockito.when(launchedEventService.getActiveEventsByPersonageId(Mockito.any()))
            .thenReturn(new CurrentEvents(List.of()));
    }

    @Test
    void Given_PersonageAlreadyInAnomaly_When_Start_Then_Error() {
        mockEligibleGroup();
        final var member = withGroup(PersonageUtils.withId(personageId), groupId);
        Mockito.when(personageService.getByIdForce(personageId)).thenReturn(member);
        Mockito.when(anomalyStorage.hasStartOnDate(Mockito.eq(groupId), Mockito.any())).thenReturn(false);
        Mockito.when(launchedEventService.getActiveEventsByPersonageId(personageId))
            .thenReturn(new CurrentEvents(List.of(
                new CurrentEvent(999L, EventType.ANOMALY, TimeUtils.moscowTime().plusHours(1))
            )));

        final var result = service.start(groupId, personageId, AnomalyMode.SAFE);

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(AnomalyError.AlreadyInAnomaly.INSTANCE, result.getLeft());
    }

    @Test
    void Given_PersonageAlreadyInOtherAnomaly_When_Join_Then_Error() {
        final var member = withGroup(PersonageUtils.withId(otherPersonageId), groupId);
        final var anomaly = new Anomaly.Dangerous.Gathering(106L, groupId, personageId);
        final var event = new LaunchedEvent(
            106L,
            5,
            TimeUtils.moscowTime(),
            TimeUtils.moscowTime().plusHours(1),
            EventStatus.LAUNCHED,
            Optional.empty()
        );
        mockEvent(event);
        Mockito.when(anomalyStorage.findByLaunchedEventId(106L)).thenReturn(Optional.of(anomaly));
        Mockito.when(personageService.getByIdForce(otherPersonageId)).thenReturn(member);
        Mockito.when(launchedEventService.getActiveEventsByPersonageId(otherPersonageId))
            .thenReturn(new CurrentEvents(List.of(
                new CurrentEvent(999L, EventType.ANOMALY, TimeUtils.moscowTime().plusHours(1))
            )));

        final var result = service.join(106L, otherPersonageId);

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(AnomalyError.AlreadyInAnomaly.INSTANCE, result.getLeft());
        Mockito.verify(personageEventService, Mockito.never())
            .addPersonageToLaunchedEventAssumingLocked(Mockito.any());
    }

    @Test
    void Given_AlreadyStartedToday_When_Start_Then_Error() {
        mockEligibleGroup();
        final var member = withGroup(PersonageUtils.withId(personageId), groupId);
        Mockito.when(personageService.getByIdForce(personageId)).thenReturn(member);
        Mockito.when(anomalyStorage.hasStartOnDate(Mockito.eq(groupId), Mockito.any()))
            .thenReturn(true);

        final var result = service.start(groupId, personageId, AnomalyMode.SAFE);

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(AnomalyError.AlreadyStartedToday.INSTANCE, result.getLeft());
    }

    @Test
    void Given_GatheringSafeWithFourMembers_When_FifthJoins_Then_StayGathering() {
        final var joiningId = PersonageId.from(5L);
        final var joining = withGroup(PersonageUtils.withId(joiningId), groupId);
        final var anomaly = new Anomaly.Safe(
            100L,
            groupId,
            personageId,
            AnomalyPveTemplate.CRYSTAL_STORM,
            Anomaly.Safe.Phase.GATHERING
        );
        final var event = launchedEvent(100L);
        mockEvent(event);
        Mockito.when(anomalyStorage.findByLaunchedEventId(100L)).thenReturn(Optional.of(anomaly));
        Mockito.when(personageService.getByIdForce(joiningId)).thenReturn(joining);
        Mockito.when(personageEventService.getParticipants(100L)).thenReturn(members(4));

        final var result = service.join(100L, joiningId);

        Assertions.assertTrue(result.isRight());
        Mockito.verify(personageEventService).addPersonageToLaunchedEventAssumingLocked(Mockito.any());
        Mockito.verify(anomalyStorage, Mockito.never()).update(Mockito.any());
        Mockito.verify(launchedEventService, Mockito.never()).updateEndDate(Mockito.anyLong(), Mockito.any());
    }

    @Test
    void Given_DangerousIncompleteParty_When_Join_Then_StayGathering() {
        final var joining = withGroup(PersonageUtils.withId(otherPersonageId), groupId);
        final var anomaly = new Anomaly.Dangerous.Gathering(101L, groupId, personageId);
        final var event = launchedEvent(101L);
        mockEvent(event);
        Mockito.when(anomalyStorage.findByLaunchedEventId(101L)).thenReturn(Optional.of(anomaly));
        Mockito.when(personageService.getByIdForce(otherPersonageId)).thenReturn(joining);
        Mockito.when(personageEventService.getParticipants(101L)).thenReturn(members(1));

        final var result = service.join(101L, otherPersonageId);

        Assertions.assertTrue(result.isRight());
        Mockito.verify(personageEventService).addPersonageToLaunchedEventAssumingLocked(Mockito.any());
        Mockito.verify(anomalyStorage, Mockito.never()).update(Mockito.any());
        Mockito.verify(launchedEventService, Mockito.never()).updateEndDate(Mockito.anyLong(), Mockito.any());
    }

    @Test
    void Given_DangerousWithFourMembers_When_FifthJoins_Then_StayGathering() {
        final var joiningId = PersonageId.from(5L);
        final var joining = withGroup(PersonageUtils.withId(joiningId), groupId);
        final var anomaly = new Anomaly.Dangerous.Gathering(103L, groupId, personageId);
        final var event = launchedEvent(103L);
        mockEvent(event);
        Mockito.when(anomalyStorage.findByLaunchedEventId(103L)).thenReturn(Optional.of(anomaly));
        Mockito.when(personageService.getByIdForce(joiningId)).thenReturn(joining);
        Mockito.when(personageEventService.getParticipants(103L)).thenReturn(members(4));

        final var result = service.join(103L, joiningId);

        Assertions.assertTrue(result.isRight());
        Mockito.verify(personageEventService).addPersonageToLaunchedEventAssumingLocked(Mockito.any());
        Mockito.verify(anomalyStorage, Mockito.never()).update(Mockito.any());
        Mockito.verify(launchedEventService, Mockito.never()).updateEndDate(Mockito.anyLong(), Mockito.any());
    }

    @Test
    void Given_GatheringSafeWithMembers_When_Ready_Then_StartedPveWaiting() {
        final var member = withGroup(PersonageUtils.withId(personageId), groupId);
        final var anomaly = new Anomaly.Safe(
            104L,
            groupId,
            personageId,
            AnomalyPveTemplate.CRYSTAL_STORM,
            Anomaly.Safe.Phase.GATHERING
        );
        final var event = launchedEvent(104L);
        mockEvent(event);
        Mockito.when(anomalyStorage.findByLaunchedEventId(104L)).thenReturn(Optional.of(anomaly));
        Mockito.when(personageEventService.getParticipants(104L))
            .thenReturn(List.of(new EventParticipant(member, Optional.empty())));

        final var result = service.ready(104L, personageId);

        Assertions.assertTrue(result.isRight());
        Assertions.assertInstanceOf(AnomalyService.AnomalyReadyResult.StartedPveWaiting.class, result.get());
        Mockito.verify(anomalyStorage).update(Mockito.argThat(updated ->
            updated instanceof Anomaly.Safe safe
                && safe.phase() == Anomaly.Safe.Phase.PVE_WAITING
        ));
        Mockito.verify(launchedEventService).updateEndDate(Mockito.eq(104L), Mockito.any());
    }

    @Test
    void Given_DangerousIncompleteParty_When_Ready_Then_PartyNotFull() {
        final var owner = withGroup(PersonageUtils.withId(personageId), groupId);
        final var anomaly = new Anomaly.Dangerous.Gathering(105L, groupId, personageId);
        final var event = launchedEvent(105L);
        mockEvent(event);
        Mockito.when(anomalyStorage.findByLaunchedEventId(105L)).thenReturn(Optional.of(anomaly));
        Mockito.when(personageEventService.getParticipants(105L))
            .thenReturn(List.of(new EventParticipant(owner, Optional.empty())));

        final var result = service.ready(105L, personageId);

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(AnomalyError.PartyNotFull.INSTANCE, result.getLeft());
    }

    @Test
    void Given_DangerousFullParty_When_Ready_Then_StartedSearching() {
        final var anomaly = new Anomaly.Dangerous.Gathering(112L, groupId, personageId);
        final var event = launchedEvent(112L);
        mockEvent(event);
        Mockito.when(anomalyStorage.findByLaunchedEventId(112L)).thenReturn(Optional.of(anomaly));
        Mockito.when(personageEventService.getParticipants(112L)).thenReturn(members(5));
        Mockito.when(gvgStorage.getRating(groupId)).thenReturn(1000);

        final var result = service.ready(112L, personageId);

        Assertions.assertTrue(result.isRight());
        Assertions.assertInstanceOf(AnomalyService.AnomalyReadyResult.StartedSearching.class, result.get());
        Mockito.verify(anomalyStorage).update(Mockito.argThat(updated ->
            updated instanceof Anomaly.Dangerous.Searching
        ));
        Mockito.verify(launchedEventService).updateEndDate(Mockito.eq(112L), Mockito.any());
    }

    @Test
    void Given_SafeGatheringExpiredWithMembers_When_ProcessExpired_Then_StartedPveWaiting() {
        final var anomaly = new Anomaly.Safe(
            113L,
            groupId,
            personageId,
            AnomalyPveTemplate.CRYSTAL_STORM,
            Anomaly.Safe.Phase.GATHERING
        );
        final var event = expiredEvent(113L);
        Mockito.when(anomalyStorage.findByLaunchedEventId(113L)).thenReturn(Optional.of(anomaly));
        Mockito.when(personageEventService.getParticipants(113L)).thenReturn(members(2));
        mockEvent(event);

        final var result = service.processExpired(event);

        Assertions.assertInstanceOf(EventResult.AnomalyResult.GatheringStarted.class, result);
        Mockito.verify(anomalyStorage).update(Mockito.argThat(updated ->
            updated instanceof Anomaly.Safe safe
                && safe.phase() == Anomaly.Safe.Phase.PVE_WAITING
        ));
        Mockito.verify(launchedEventService).updateEndDate(Mockito.eq(113L), Mockito.any());
        Mockito.verify(launchedEventService, Mockito.never()).updateStatus(Mockito.anyLong(), Mockito.any());
    }

    @Test
    void Given_DangerousGatheringExpiredIncomplete_When_ProcessExpired_Then_ExpiredGathering() {
        final var anomaly = new Anomaly.Dangerous.Gathering(114L, groupId, personageId);
        final var event = expiredEvent(114L);
        mockEvent(event);
        Mockito.when(anomalyStorage.findByLaunchedEventId(114L)).thenReturn(Optional.of(anomaly));
        Mockito.when(personageEventService.getParticipants(114L)).thenReturn(members(3));

        final var result = service.processExpired(event);

        Assertions.assertEquals(EventResult.AnomalyResult.ExpiredGathering.INSTANCE, result);
        Mockito.verify(launchedEventService).updateStatus(114L, EventStatus.EXPIRED);
        Mockito.verify(anomalyStorage, Mockito.never()).update(Mockito.any());
    }

    @Test
    void Given_DangerousGatheringExpiredFull_When_ProcessExpired_Then_StartedSearching() {
        final var anomaly = new Anomaly.Dangerous.Gathering(115L, groupId, personageId);
        final var event = expiredEvent(115L);
        Mockito.when(anomalyStorage.findByLaunchedEventId(115L)).thenReturn(Optional.of(anomaly));
        Mockito.when(personageEventService.getParticipants(115L)).thenReturn(members(5));
        Mockito.when(gvgStorage.getRating(groupId)).thenReturn(1000);
        mockEvent(event);

        final var result = service.processExpired(event);

        Assertions.assertInstanceOf(EventResult.AnomalyResult.GatheringStarted.class, result);
        Mockito.verify(anomalyStorage).update(Mockito.argThat(updated ->
            updated instanceof Anomaly.Dangerous.Searching
        ));
        Mockito.verify(launchedEventService).updateEndDate(Mockito.eq(115L), Mockito.any());
        Mockito.verify(launchedEventService, Mockito.never()).updateStatus(Mockito.anyLong(), Mockito.any());
    }

    @Test
    void Given_SearchingExpired_When_ProcessExpired_Then_PveFallback() {
        final var anomaly = new Anomaly.Dangerous.Searching(
            102L,
            groupId,
            personageId,
            1000
        );
        final var event = new LaunchedEvent(
            102L,
            5,
            TimeUtils.moscowTime().minusHours(6),
            TimeUtils.moscowTime().minusMinutes(1),
            EventStatus.LAUNCHED,
            Optional.empty()
        );
        final var pveResult = new EventResult.AnomalyResult.PveBattleFinished(
            102L,
            groupId,
            true,
            AnomalyReward.of(20, 2),
            List.of(),
            List.of()
        );
        mockEvent(event);
        Mockito.when(anomalyStorage.findByLaunchedEventId(102L)).thenReturn(Optional.of(anomaly));
        Mockito.when(anomalyBattleService.fightPveFallback(event, groupId)).thenReturn(pveResult);

        final var result = service.processExpired(event);

        Assertions.assertInstanceOf(EventResult.AnomalyResult.PveBattleFinished.class, result);
        Mockito.verify(anomalyBattleService).fightPveFallback(event, groupId);
    }

    @Test
    void Given_AcceptedWithoutWinner_When_ProcessExpired_Then_RetryFight() {
        final var opponentGroupId = GroupId.from(20L);
        final var anomaly = new Anomaly.Dangerous.Accepted(
            108L,
            groupId,
            personageId,
            opponentGroupId,
            otherPersonageId,
            Optional.of(109L),
            Optional.empty(),
            1000
        );
        final var event = new LaunchedEvent(
            108L,
            5,
            TimeUtils.moscowTime().minusHours(6),
            TimeUtils.moscowTime().minusMinutes(1),
            EventStatus.LAUNCHED,
            Optional.empty()
        );
        final var battleResult = new EventResult.AnomalyResult.BattleFinished(
            108L, groupId, opponentGroupId, List.of(), List.of()
        );
        mockEvent(event);
        Mockito.when(anomalyStorage.findByLaunchedEventId(108L)).thenReturn(Optional.of(anomaly));
        Mockito.when(anomalyBattleService.fight(event, anomaly)).thenReturn(battleResult);

        final var result = service.processExpired(event);

        Assertions.assertEquals(battleResult, result);
        Mockito.verify(anomalyBattleService).fight(event, anomaly);
    }

    @Test
    void Given_AcceptedWithWinner_When_ProcessExpired_Then_AlreadyFinal() {
        final var opponentGroupId = GroupId.from(20L);
        final var anomaly = new Anomaly.Dangerous.Accepted(
            110L,
            groupId,
            personageId,
            opponentGroupId,
            otherPersonageId,
            Optional.of(111L),
            Optional.of(groupId),
            1000
        );
        final var event = new LaunchedEvent(
            110L,
            5,
            TimeUtils.moscowTime().minusHours(6),
            TimeUtils.moscowTime().minusMinutes(1),
            EventStatus.LAUNCHED,
            Optional.empty()
        );
        mockEvent(event);
        Mockito.when(anomalyStorage.findByLaunchedEventId(110L)).thenReturn(Optional.of(anomaly));

        final var result = service.processExpired(event);

        Assertions.assertEquals(EventResult.AnomalyResult.AlreadyFinal.INSTANCE, result);
        Mockito.verify(anomalyBattleService, Mockito.never()).fight(Mockito.any(), Mockito.any());
        Mockito.verify(anomalyBattleService, Mockito.never()).fightPveFallback(Mockito.any(), Mockito.any());
    }

    @Test
    void Given_Searching_When_Join_Then_RosterLocked() {
        final var member = withGroup(PersonageUtils.withId(otherPersonageId), groupId);
        final var anomaly = new Anomaly.Dangerous.Searching(
            107L,
            groupId,
            personageId,
            1000
        );
        final var event = new LaunchedEvent(
            107L,
            5,
            TimeUtils.moscowTime(),
            TimeUtils.moscowTime().plusHours(11),
            EventStatus.LAUNCHED,
            Optional.empty()
        );
        mockEvent(event);
        Mockito.when(anomalyStorage.findByLaunchedEventId(107L)).thenReturn(Optional.of(anomaly));
        Mockito.when(personageService.getByIdForce(otherPersonageId)).thenReturn(member);

        final var result = service.join(107L, otherPersonageId);

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(AnomalyError.RosterLocked.INSTANCE, result.getLeft());
    }

    @Test
    void Given_ExtendedDeadline_When_ProcessStaleExpiredSnapshot_Then_NotDueWithoutReadingPhase() {
        final var snapshot = expiredEvent(116L);
        final var current = launchedEvent(snapshot.id());
        mockEvent(current);

        Assertions.assertEquals(EventResult.AnomalyResult.NotDue.INSTANCE, service.processExpired(snapshot));

        Mockito.verify(launchedEventService).getByIdForUpdate(snapshot.id());
        Mockito.verifyNoMoreInteractions(launchedEventService);
        Mockito.verifyNoInteractions(anomalyStorage, anomalyBattleService, personageEventService, personageService);
    }

    @ParameterizedTest
    @EnumSource(value = EventStatus.class, names = "LAUNCHED", mode = EnumSource.Mode.EXCLUDE)
    void Given_FreshFinalStatus_When_ProcessStaleActiveSnapshot_Then_AlreadyFinal(EventStatus status) {
        final var snapshot = expiredEvent(117L);
        mockEvent(new LaunchedEvent(snapshot.id(), snapshot.eventId(), snapshot.startDate(), snapshot.endDate(),
            status, snapshot.eventParams()));

        Assertions.assertEquals(EventResult.AnomalyResult.AlreadyFinal.INSTANCE, service.processExpired(snapshot));

        Mockito.verify(launchedEventService).getByIdForUpdate(snapshot.id());
        Mockito.verifyNoMoreInteractions(launchedEventService);
        Mockito.verifyNoInteractions(anomalyStorage, anomalyBattleService, personageEventService, personageService);
    }

    @Test
    void Given_MissingFreshEvent_When_ProcessExpired_Then_AlreadyFinalWithoutWrites() {
        final var snapshot = expiredEvent(118L);
        Mockito.when(launchedEventService.getByIdForUpdate(snapshot.id())).thenReturn(Optional.empty());

        Assertions.assertEquals(EventResult.AnomalyResult.AlreadyFinal.INSTANCE, service.processExpired(snapshot));

        Mockito.verify(launchedEventService).getByIdForUpdate(snapshot.id());
        Mockito.verifyNoMoreInteractions(launchedEventService);
        Mockito.verifyNoInteractions(anomalyStorage, anomalyBattleService, personageEventService, personageService);
    }

    @Test
    void Given_FinalSnapshotButFreshActiveEvent_When_ProcessExpired_Then_FightsUsingFreshEvent() {
        final var current = expiredEvent(119L);
        final var snapshot = new LaunchedEvent(current.id(), current.eventId(), current.startDate(), current.endDate(),
            EventStatus.SUCCESS, current.eventParams());
        final var safe = new Anomaly.Safe(current.id(), groupId, personageId, AnomalyPveTemplate.ASH_WIND,
            Anomaly.Safe.Phase.PVE_WAITING);
        final var expected = new EventResult.AnomalyResult.PveBattleFinished(current.id(), groupId, true,
            AnomalyReward.of(10, 2), List.of(), List.of());
        mockEvent(current);
        Mockito.when(anomalyStorage.findByLaunchedEventId(current.id())).thenReturn(Optional.of(safe));
        Mockito.when(anomalyBattleService.fightPve(current, safe)).thenReturn(expected);

        Assertions.assertSame(expected, service.processExpired(snapshot));

        final var order = Mockito.inOrder(launchedEventService, anomalyStorage, anomalyBattleService);
        order.verify(launchedEventService).getByIdForUpdate(current.id());
        order.verify(anomalyStorage).findByLaunchedEventId(current.id());
        order.verify(anomalyBattleService).fightPve(current, safe);
        Mockito.verify(anomalyBattleService, Mockito.never()).fightPve(Mockito.eq(snapshot), Mockito.any());
    }

    @Test
    void Given_ExpiredEventWithoutAnomaly_When_ProcessExpired_Then_ExpiresOnlyThatEvent() {
        final var current = expiredEvent(120L);
        mockEvent(current);
        Mockito.when(anomalyStorage.findByLaunchedEventId(current.id())).thenReturn(Optional.empty());

        Assertions.assertEquals(EventResult.AnomalyResult.ExpiredGathering.INSTANCE, service.processExpired(current));

        Mockito.verify(launchedEventService).updateStatus(current.id(), EventStatus.EXPIRED);
        Mockito.verifyNoInteractions(anomalyBattleService, personageEventService, personageService);
    }

    @Test
    void Given_FreshFinalEvent_When_JoinOrReady_Then_RejectBeforeReadingPhaseOrPersonage() {
        final var current = expiredEvent(121L);
        mockEvent(new LaunchedEvent(current.id(), current.eventId(), current.startDate(), current.endDate(),
            EventStatus.CANCELED, current.eventParams()));

        Assertions.assertEquals(AnomalyError.FinalStatus.INSTANCE, service.join(current.id(), personageId).getLeft());
        Assertions.assertEquals(AnomalyError.FinalStatus.INSTANCE, service.ready(current.id(), personageId).getLeft());

        Mockito.verify(launchedEventService, Mockito.times(2)).getByIdForUpdate(current.id());
        Mockito.verifyNoMoreInteractions(launchedEventService);
        Mockito.verifyNoInteractions(anomalyStorage, anomalyBattleService, personageEventService, personageService);
    }

    private void mockEvent(LaunchedEvent event) {
        Mockito.when(launchedEventService.getByIdForUpdate(event.id())).thenReturn(Optional.of(event));
        Mockito.when(launchedEventService.getById(event.id())).thenReturn(Optional.of(event));
    }

    private LaunchedEvent expiredEvent(long id) {
        return new LaunchedEvent(id, 5, TimeUtils.moscowTime().minusHours(2),
            TimeUtils.moscowTime().minusMinutes(1), EventStatus.LAUNCHED, Optional.empty());
    }

    private void mockEligibleGroup() {
        final var group = new Group(
            groupId,
            Optional.of("TAG"),
            "Test",
            BadgeView.STANDARD,
            true,
            Mockito.mock(GroupSettings.class),
            1
        );
        Mockito.when(getGroup.forceGet(groupId)).thenReturn(group);
        Mockito.when(outpostStorage.findBuildingSlot(groupId, Building.STORM_SCANNER))
            .thenReturn(Optional.of(new OutpostSlot.BuildingSlot(
                groupId, Building.STORM_SCANNER, 1, Optional.empty(), 0
            )));
        Mockito.when(anomalyStorage.hasActiveAnomaly(groupId)).thenReturn(false);
    }

    private LaunchedEvent launchedEvent(long id) {
        return new LaunchedEvent(
            id,
            5,
            TimeUtils.moscowTime(),
            TimeUtils.moscowTime().plusHours(1),
            EventStatus.LAUNCHED,
            Optional.empty()
        );
    }

    private List<EventParticipant> members(int count) {
        final var list = new ArrayList<EventParticipant>();
        for (int i = 1; i <= count; ++i) {
            list.add(new EventParticipant(
                withGroup(PersonageUtils.withId(PersonageId.from(i)), groupId),
                Optional.empty()
            ));
        }
        return list;
    }

    private static ru.homyakin.seeker.game.personage.models.Personage withGroup(
        ru.homyakin.seeker.game.personage.models.Personage base,
        GroupId groupId
    ) {
        return new ru.homyakin.seeker.game.personage.models.Personage(
            base.id(),
            base.name(),
            Optional.of("TAG"),
            Optional.of(groupId),
            base.money(),
            base.stormShards(),
            base.energy(),
            base.badge(),
            base.effects(),
            base.position(),
            base.targetingTactic(),
            base.onlineStreak()
        );
    }
}
