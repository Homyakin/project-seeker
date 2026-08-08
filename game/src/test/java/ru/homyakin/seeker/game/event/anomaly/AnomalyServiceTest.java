package ru.homyakin.seeker.game.event.anomaly;

import io.vavr.control.Either;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
        Mockito.when(launchedEventService.getById(106L)).thenReturn(Optional.of(event));
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
    void Given_GatheringSafeWithMembers_When_Ready_Then_StartedPveWaiting() {
        final var member = withGroup(PersonageUtils.withId(personageId), groupId);
        final var anomaly = new Anomaly.Safe(
            100L,
            groupId,
            personageId,
            AnomalyPveTemplate.CRYSTAL_STORM,
            Anomaly.Safe.Phase.GATHERING
        );
        final var event = new LaunchedEvent(
            100L,
            5,
            TimeUtils.moscowTime(),
            TimeUtils.moscowTime().plusHours(1),
            EventStatus.LAUNCHED,
            Optional.empty()
        );
        Mockito.when(launchedEventService.getById(100L)).thenReturn(Optional.of(event));
        Mockito.when(anomalyStorage.findByLaunchedEventId(100L)).thenReturn(Optional.of(anomaly));
        Mockito.when(personageEventService.getParticipants(100L))
            .thenReturn(List.of(new EventParticipant(member, Optional.empty())));

        final var result = service.ready(100L, personageId);

        Assertions.assertTrue(result.isRight());
        Assertions.assertInstanceOf(AnomalyService.AnomalyReadyResult.StartedPveWaiting.class, result.get());
        Mockito.verify(anomalyStorage).update(Mockito.argThat(updated ->
            updated instanceof Anomaly.Safe safe
                && safe.phase() == Anomaly.Safe.Phase.PVE_WAITING
        ));
        Mockito.verify(launchedEventService).updateEndDate(Mockito.eq(100L), Mockito.any());
    }

    @Test
    void Given_DangerousIncompleteParty_When_Ready_Then_PartyNotFull() {
        final var owner = withGroup(PersonageUtils.withId(personageId), groupId);
        final var anomaly = new Anomaly.Dangerous.Gathering(101L, groupId, personageId);
        final var event = new LaunchedEvent(
            101L,
            5,
            TimeUtils.moscowTime(),
            TimeUtils.moscowTime().plusHours(1),
            EventStatus.LAUNCHED,
            Optional.empty()
        );
        Mockito.when(launchedEventService.getById(101L)).thenReturn(Optional.of(event));
        Mockito.when(anomalyStorage.findByLaunchedEventId(101L)).thenReturn(Optional.of(anomaly));
        Mockito.when(personageEventService.getParticipants(101L))
            .thenReturn(List.of(new EventParticipant(owner, Optional.empty())));

        final var result = service.ready(101L, personageId);

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(AnomalyError.PartyNotFull.INSTANCE, result.getLeft());
    }

    @Test
    void Given_SearchingExpired_When_ProcessExpired_Then_PveFallback() {
        final var anomaly = new Anomaly.Dangerous.Searching(
            102L,
            groupId,
            personageId,
            1000,
            TimeUtils.moscowTime().minusMinutes(1)
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
            1000,
            TimeUtils.moscowTime().minusMinutes(1)
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
            1000,
            TimeUtils.moscowTime().minusMinutes(1)
        );
        final var event = new LaunchedEvent(
            110L,
            5,
            TimeUtils.moscowTime().minusHours(6),
            TimeUtils.moscowTime().minusMinutes(1),
            EventStatus.LAUNCHED,
            Optional.empty()
        );
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
            1000,
            TimeUtils.moscowTime().plusHours(11)
        );
        final var event = new LaunchedEvent(
            107L,
            5,
            TimeUtils.moscowTime(),
            TimeUtils.moscowTime().plusHours(11),
            EventStatus.LAUNCHED,
            Optional.empty()
        );
        Mockito.when(launchedEventService.getById(107L)).thenReturn(Optional.of(event));
        Mockito.when(anomalyStorage.findByLaunchedEventId(107L)).thenReturn(Optional.of(anomaly));
        Mockito.when(personageService.getByIdForce(otherPersonageId)).thenReturn(member);

        final var result = service.join(107L, otherPersonageId);

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(AnomalyError.RosterLocked.INSTANCE, result.getLeft());
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
            base.onlineStreak()
        );
    }
}
