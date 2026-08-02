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
import ru.homyakin.seeker.game.event.anomaly.action.AnomalyMatchmaker;
import ru.homyakin.seeker.game.event.anomaly.entity.Anomaly;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyConfig;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyGvgStorage;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyStorage;
import ru.homyakin.seeker.game.event.anomaly.entity.SendAnomalyChallengeToGroup;
import ru.homyakin.seeker.game.event.launched.LaunchedEvent;
import ru.homyakin.seeker.game.event.launched.LaunchedEventService;
import ru.homyakin.seeker.game.event.models.EventStatus;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.infrastructure.lock.InMemoryLockService;
import ru.homyakin.seeker.utils.TimeUtils;

public class AnomalyMatchmakerTest {
    private final AnomalyStorage anomalyStorage = Mockito.mock();
    private final AnomalyGvgStorage gvgStorage = Mockito.mock();
    private final AnomalyConfig config = Mockito.mock();
    private final LaunchedEventService launchedEventService = Mockito.mock();
    private final SendAnomalyChallengeToGroup sendAnomalyChallengeToGroup = Mockito.mock();

    private AnomalyMatchmaker matchmaker;

    private final GroupId initiatorGroupId = GroupId.from(10L);
    private final GroupId targetGroupId = GroupId.from(20L);
    private final PersonageId ownerId = PersonageId.from(1L);
    private final long eventId = 100L;

    @BeforeEach
    void init() {
        matchmaker = new AnomalyMatchmaker(
            anomalyStorage,
            gvgStorage,
            config,
            launchedEventService,
            sendAnomalyChallengeToGroup,
            new InMemoryLockService()
        );
        Mockito.when(config.dangerousSearchDuration()).thenReturn(Duration.ofHours(12));
        Mockito.when(config.dangerousChallengeDuration()).thenReturn(Duration.ofHours(1));
        Mockito.when(config.recentMeetPenaltyFirstDay()).thenReturn(256);
    }

    @Test
    void Given_EligibleTarget_When_Match_Then_AssignsOpponentAndSendsChallenge() {
        final var searching = searchingAnomaly();
        final var event = launchedEvent();
        mockSearchingEvent(searching, event);
        Mockito.when(gvgStorage.findEligibleChallengeTargets(Mockito.eq(initiatorGroupId), Mockito.any()))
            .thenReturn(List.of(targetGroupId));
        Mockito.when(gvgStorage.getRating(targetGroupId)).thenReturn(1000);
        Mockito.when(gvgStorage.findOpponentFoughtAtList(initiatorGroupId, targetGroupId))
            .thenReturn(List.of());
        Mockito.when(anomalyStorage.hasActiveAnomaly(targetGroupId)).thenReturn(false);
        Mockito.when(anomalyStorage.tryAssignOpponent(Mockito.any(), Mockito.any())).thenReturn(true);
        Mockito.when(launchedEventService.getById(eventId)).thenReturn(Optional.of(event));

        matchmaker.matchSearchingExpeditions();

        final var challengedCaptor = ArgumentCaptor.forClass(Anomaly.Dangerous.Challenged.class);
        Mockito.verify(anomalyStorage).tryAssignOpponent(challengedCaptor.capture(), Mockito.eq(TimeUtils.moscowDate()));
        Assertions.assertEquals(targetGroupId, challengedCaptor.getValue().opponentGroupId());
        Mockito.verify(launchedEventService).addGroupToEvent(eventId, targetGroupId);
        Mockito.verify(sendAnomalyChallengeToGroup).send(targetGroupId, event);
    }

    @Test
    void Given_ChallengeAlreadyUsedToday_When_Match_Then_SkipsTarget() {
        final var searching = searchingAnomaly();
        final var event = launchedEvent();
        mockSearchingEvent(searching, event);
        Mockito.when(gvgStorage.findEligibleChallengeTargets(Mockito.eq(initiatorGroupId), Mockito.any()))
            .thenReturn(List.of(targetGroupId));
        Mockito.when(gvgStorage.getRating(targetGroupId)).thenReturn(1000);
        Mockito.when(gvgStorage.findOpponentFoughtAtList(initiatorGroupId, targetGroupId))
            .thenReturn(List.of());
        Mockito.when(anomalyStorage.hasActiveAnomaly(targetGroupId)).thenReturn(false);
        Mockito.when(anomalyStorage.tryAssignOpponent(Mockito.any(), Mockito.any())).thenReturn(false);

        matchmaker.matchSearchingExpeditions();

        Mockito.verify(anomalyStorage).tryAssignOpponent(Mockito.any(), Mockito.eq(TimeUtils.moscowDate()));
        Mockito.verify(launchedEventService, Mockito.never()).addGroupToEvent(Mockito.anyLong(), Mockito.any());
        Mockito.verifyNoInteractions(sendAnomalyChallengeToGroup);
    }

    private void mockSearchingEvent(Anomaly.Dangerous.Searching searching, LaunchedEvent event) {
        Mockito.when(anomalyStorage.findActiveSearchingWithoutOpponent()).thenReturn(List.of(event));
        Mockito.when(launchedEventService.getById(eventId)).thenReturn(Optional.of(event));
        Mockito.when(anomalyStorage.findByLaunchedEventId(eventId)).thenReturn(Optional.of(searching));
    }

    private Anomaly.Dangerous.Searching searchingAnomaly() {
        return new Anomaly.Dangerous.Searching(
            eventId,
            initiatorGroupId,
            ownerId,
            1000,
            TimeUtils.moscowTime().plusHours(11)
        );
    }

    private LaunchedEvent launchedEvent() {
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
