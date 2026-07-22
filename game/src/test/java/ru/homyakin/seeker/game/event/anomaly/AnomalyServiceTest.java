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
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyPhase;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyStorage;
import ru.homyakin.seeker.game.event.launched.LaunchedEvent;
import ru.homyakin.seeker.game.event.launched.LaunchedEventService;
import ru.homyakin.seeker.game.event.models.EventStatus;
import ru.homyakin.seeker.game.event.service.EventService;
import ru.homyakin.seeker.game.group.action.GetGroup;
import ru.homyakin.seeker.game.group.entity.Group;
import ru.homyakin.seeker.game.group.entity.GroupSettings;
import ru.homyakin.seeker.game.models.Money;
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
        Mockito.when(config.dangerousSearchDuration()).thenReturn(Duration.ofHours(12));
        Mockito.when(config.safeReward()).thenReturn(Money.from(10));
        Mockito.when(config.noMatchReward()).thenReturn(Money.from(8));
        Mockito.when(lockService.tryLockAndCalc(Mockito.anyString(), Mockito.any()))
            .thenAnswer(invocation -> Either.right(
                invocation.getArgument(1, java.util.function.Supplier.class).get()
            ));
    }

    @Test
    void Given_AlreadyStartedToday_When_Start_Then_Error() {
        mockEligibleGroup();
        final var member = withGroup(PersonageUtils.withId(personageId), groupId);
        Mockito.when(personageService.getByIdForce(personageId)).thenReturn(member);
        Mockito.when(anomalyStorage.hasStartOnDate(Mockito.eq(groupId), Mockito.any()))
            .thenReturn(true);

        final var result = service.start(groupId, personageId);

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(AnomalyError.AlreadyStartedToday.INSTANCE, result.getLeft());
    }

    @Test
    void Given_GatheringSafeWithMembers_When_Ready_Then_SafeCompleted() {
        final var member = withGroup(PersonageUtils.withId(personageId), groupId);
        final var anomaly = new Anomaly(
            100L,
            groupId,
            Optional.of(personageId),
            AnomalyPhase.GATHERING,
            Optional.of(AnomalyMode.SAFE),
            "storm-surge",
            false,
            Optional.empty(),
            Optional.empty(),
            false
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
        Assertions.assertInstanceOf(AnomalyService.AnomalyReadyResult.SafeCompleted.class, result.get());
        Mockito.verify(personageService).addMoney(member, Money.from(10));
        Mockito.verify(launchedEventService).updateStatus(100L, EventStatus.SUCCESS);
    }

    @Test
    void Given_DangerousIncompleteParty_When_Ready_Then_PartyNotFull() {
        final var owner = withGroup(PersonageUtils.withId(personageId), groupId);
        final var anomaly = new Anomaly(
            101L,
            groupId,
            Optional.of(personageId),
            AnomalyPhase.GATHERING,
            Optional.of(AnomalyMode.DANGEROUS),
            "storm-surge",
            false,
            Optional.empty(),
            Optional.empty(),
            false
        );
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
    void Given_SearchingExpired_When_ProcessExpired_Then_NoMatchReward() {
        final var anomaly = new Anomaly(
            102L,
            groupId,
            Optional.of(personageId),
            AnomalyPhase.SEARCHING,
            Optional.of(AnomalyMode.DANGEROUS),
            "storm-surge",
            true,
            Optional.empty(),
            Optional.of(1000),
            false
        );
        final var event = new LaunchedEvent(
            102L,
            5,
            TimeUtils.moscowTime().minusHours(12),
            TimeUtils.moscowTime().minusMinutes(1),
            EventStatus.LAUNCHED,
            Optional.empty()
        );
        final var member = withGroup(PersonageUtils.withId(personageId), groupId);
        Mockito.when(anomalyStorage.findByLaunchedEventId(102L)).thenReturn(Optional.of(anomaly));
        Mockito.when(personageEventService.getParticipants(102L))
            .thenReturn(List.of(new EventParticipant(member, Optional.empty())));

        final var result = service.processExpired(event);

        Assertions.assertInstanceOf(
            ru.homyakin.seeker.game.event.models.EventResult.AnomalyResult.NoMatch.class,
            result
        );
        Mockito.verify(personageService).addMoney(member, Money.from(8));
        Mockito.verify(launchedEventService).updateStatus(102L, EventStatus.SUCCESS);
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
            base.energy(),
            base.badge(),
            base.effects(),
            base.position(),
            base.onlineStreak()
        );
    }
}
