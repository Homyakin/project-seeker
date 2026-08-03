package ru.homyakin.seeker.game.event.anomaly;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.event.anomaly.action.AnomalyBattleService;
import ru.homyakin.seeker.game.event.anomaly.action.AnomalyMatchmaker;
import ru.homyakin.seeker.game.event.anomaly.entity.Anomaly;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyConfig;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyGvgStorage;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyStorage;
import ru.homyakin.seeker.game.event.anomaly.entity.NotifyAnomalyBattleFinished;
import ru.homyakin.seeker.game.event.launched.LaunchedEvent;
import ru.homyakin.seeker.game.event.launched.LaunchedEventService;
import ru.homyakin.seeker.game.event.models.EventResult;
import ru.homyakin.seeker.game.event.models.EventStatus;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.infrastructure.lock.InMemoryLockService;
import ru.homyakin.seeker.utils.TimeUtils;

public class AnomalyMatchmakerTest {
    private final AnomalyStorage anomalyStorage = Mockito.mock();
    private final AnomalyGvgStorage gvgStorage = Mockito.mock();
    private final AnomalyConfig config = Mockito.mock();
    private final LaunchedEventService launchedEventService = Mockito.mock();
    private final AnomalyBattleService anomalyBattleService = Mockito.mock();
    private final NotifyAnomalyBattleFinished notifyAnomalyBattleFinished = Mockito.mock();

    private AnomalyMatchmaker matchmaker;

    private final GroupId hostGroupId = GroupId.from(10L);
    private final GroupId guestGroupId = GroupId.from(20L);
    private final PersonageId hostOwnerId = PersonageId.from(1L);
    private final PersonageId guestOwnerId = PersonageId.from(2L);
    private final long hostEventId = 100L;
    private final long guestEventId = 200L;

    @BeforeEach
    void init() {
        matchmaker = new AnomalyMatchmaker(
            anomalyStorage,
            gvgStorage,
            config,
            launchedEventService,
            anomalyBattleService,
            notifyAnomalyBattleFinished,
            new InMemoryLockService()
        );
        Mockito.when(config.dangerousSearchDuration()).thenReturn(Duration.ofHours(12));
        Mockito.when(config.recentMeetPenaltyFirstDay()).thenReturn(256);
    }

    @Test
    void Given_TwoSearchingInPool_When_Match_Then_MergesAndFights() {
        final var host = searchingAnomaly(hostEventId, hostGroupId, hostOwnerId);
        final var guest = searchingAnomaly(guestEventId, guestGroupId, guestOwnerId);
        final var hostEvent = launchedEvent(hostEventId);
        final var guestEvent = launchedEvent(guestEventId);
        mockPool(host, guest, hostEvent, guestEvent);

        final var battleResult = new EventResult.AnomalyResult.BattleFinished(
            hostEventId, hostGroupId, guestGroupId, List.of(), List.of()
        );
        Mockito.when(anomalyStorage.tryMergeSearchingInto(Mockito.any(), Mockito.eq(guestEventId)))
            .thenReturn(true);
        Mockito.when(anomalyBattleService.fight(Mockito.eq(hostEvent), Mockito.any()))
            .thenReturn(battleResult);

        matchmaker.matchSearchingExpeditions();

        final var acceptedCaptor = ArgumentCaptor.forClass(Anomaly.Dangerous.Accepted.class);
        Mockito.verify(anomalyStorage).tryMergeSearchingInto(acceptedCaptor.capture(), Mockito.eq(guestEventId));
        Assertions.assertEquals(guestGroupId, acceptedCaptor.getValue().opponentGroupId());
        Assertions.assertEquals(guestOwnerId, acceptedCaptor.getValue().opponentOwnerPersonageId());
        Mockito.verify(anomalyBattleService).fight(Mockito.eq(hostEvent), Mockito.eq(acceptedCaptor.getValue()));
        Mockito.verify(notifyAnomalyBattleFinished).notify(battleResult);
    }

    @Test
    void Given_MergeFails_When_Match_Then_SkipsBattle() {
        final var host = searchingAnomaly(hostEventId, hostGroupId, hostOwnerId);
        final var guest = searchingAnomaly(guestEventId, guestGroupId, guestOwnerId);
        final var hostEvent = launchedEvent(hostEventId);
        final var guestEvent = launchedEvent(guestEventId);
        mockPool(host, guest, hostEvent, guestEvent);

        Mockito.when(anomalyStorage.tryMergeSearchingInto(Mockito.any(), Mockito.eq(guestEventId)))
            .thenReturn(false);

        matchmaker.matchSearchingExpeditions();

        Mockito.verify(anomalyStorage).tryMergeSearchingInto(Mockito.any(), Mockito.eq(guestEventId));
        Mockito.verifyNoInteractions(anomalyBattleService);
        Mockito.verifyNoInteractions(notifyAnomalyBattleFinished);
    }

    @Test
    void Given_RatingTooFar_When_Match_Then_SkipsPartner() {
        // Fresh search (~0s age) only allows |Δrating| <= 10.
        final var searchEnd = TimeUtils.moscowTime().plus(Duration.ofHours(12));
        final var host = new Anomaly.Dangerous.Searching(
            hostEventId,
            hostGroupId,
            hostOwnerId,
            1000,
            searchEnd
        );
        final var guest = new Anomaly.Dangerous.Searching(
            guestEventId,
            guestGroupId,
            guestOwnerId,
            1100,
            searchEnd
        );
        final var hostEvent = launchedEvent(hostEventId);
        final var guestEvent = launchedEvent(guestEventId);
        mockPool(host, guest, hostEvent, guestEvent);

        matchmaker.matchSearchingExpeditions();

        Mockito.verify(anomalyStorage, Mockito.never()).tryMergeSearchingInto(Mockito.any(), Mockito.anyLong());
        Mockito.verifyNoInteractions(anomalyBattleService);
    }

    private void mockPool(
        Anomaly.Dangerous.Searching host,
        Anomaly.Dangerous.Searching guest,
        LaunchedEvent hostEvent,
        LaunchedEvent guestEvent
    ) {
        Mockito.when(anomalyStorage.findActiveSearchingWithoutOpponent())
            .thenReturn(List.of(hostEvent, guestEvent));
        Mockito.when(launchedEventService.getById(hostEventId)).thenReturn(Optional.of(hostEvent));
        Mockito.when(launchedEventService.getById(guestEventId)).thenReturn(Optional.of(guestEvent));
        Mockito.when(anomalyStorage.findByLaunchedEventId(hostEventId)).thenReturn(Optional.of(host));
        Mockito.when(anomalyStorage.findByLaunchedEventId(guestEventId)).thenReturn(Optional.of(guest));
        Mockito.when(gvgStorage.findOpponentFoughtAtList(hostGroupId, guestGroupId)).thenReturn(List.of());
        Mockito.when(gvgStorage.findOpponentFoughtAtList(guestGroupId, hostGroupId)).thenReturn(List.of());
    }

    private Anomaly.Dangerous.Searching searchingAnomaly(
        long eventId,
        GroupId groupId,
        PersonageId ownerId
    ) {
        return new Anomaly.Dangerous.Searching(
            eventId,
            groupId,
            ownerId,
            1000,
            TimeUtils.moscowTime().plusHours(11)
        );
    }

    private LaunchedEvent launchedEvent(long eventId) {
        final LocalDateTime now = TimeUtils.moscowTime();
        return new LaunchedEvent(
            eventId,
            5,
            now.minusHours(1),
            now.plusHours(11),
            EventStatus.LAUNCHED,
            Optional.empty()
        );
    }
}
