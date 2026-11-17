package ru.homyakin.seeker.game.event.anomaly;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.battle.EventBattleLogService;
import ru.homyakin.seeker.game.battle.PlayerCombatConfig;
import ru.homyakin.seeker.game.event.anomaly.action.AnomalyBattleService;
import ru.homyakin.seeker.game.event.anomaly.entity.Anomaly;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyConfig;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyGvgStorage;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyPveTemplate;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyStorage;
import ru.homyakin.seeker.game.event.launched.LaunchedEvent;
import ru.homyakin.seeker.game.event.launched.LaunchedEventService;
import ru.homyakin.seeker.game.event.models.EventResult;
import ru.homyakin.seeker.game.event.models.EventStatus;
import ru.homyakin.seeker.game.item.loadout.action.EquipmentLoadoutService;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.event.PersonageEventService;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.utils.TimeUtils;

/** Branch tests only; PostgreSQL integration tests prove locking and commit boundaries. */
class AnomalyBattleCompletionGuardTest {
    private static final long EVENT_ID = 130L;
    private static final GroupId GROUP = GroupId.from(10);
    private static final GroupId OPPONENT = GroupId.from(20);
    private static final PersonageId OWNER = PersonageId.from(1);
    private static final PersonageId OPPONENT_OWNER = PersonageId.from(2);

    private final PersonageEventService participants = Mockito.mock();
    private final PersonageService personages = Mockito.mock();
    private final EquipmentLoadoutService loadouts = Mockito.mock();
    private final EventBattleLogService logs = Mockito.mock();
    private final LaunchedEventService events = Mockito.mock();
    private final AnomalyConfig config = Mockito.mock();
    private final AnomalyGvgStorage ratings = Mockito.mock();
    private final AnomalyStorage anomalies = Mockito.mock();
    private final AnomalyBattleService service = Mockito.spy(new AnomalyBattleService(
        participants, personages, loadouts, logs, events, config, ratings, anomalies, new PlayerCombatConfig()
    ));

    @Test
    void missingEventDoesNotReadPhaseOrFight() {
        Mockito.when(events.getByIdForUpdate(EVENT_ID)).thenReturn(Optional.empty());

        Assertions.assertTrue(service.fightAcceptedIfActive(EVENT_ID).isEmpty());

        Mockito.verify(events).getByIdForUpdate(EVENT_ID);
        Mockito.verifyNoMoreInteractions(events);
        Mockito.verifyNoInteractions(anomalies);
        assertNoFightOrWrites();
    }

    @ParameterizedTest
    @EnumSource(value = EventStatus.class, names = "LAUNCHED", mode = EnumSource.Mode.EXCLUDE)
    void freshFinalEventDoesNotReadPhaseOrFight(EventStatus status) {
        Mockito.when(events.getByIdForUpdate(EVENT_ID)).thenReturn(Optional.of(event(status)));

        Assertions.assertTrue(service.fightAcceptedIfActive(EVENT_ID).isEmpty());

        Mockito.verifyNoInteractions(anomalies);
        assertNoFightOrWrites();
    }

    @Test
    void missingAnomalyDoesNotFight() {
        Mockito.when(events.getByIdForUpdate(EVENT_ID)).thenReturn(Optional.of(event(EventStatus.LAUNCHED)));
        Mockito.when(anomalies.findByLaunchedEventId(EVENT_ID)).thenReturn(Optional.empty());

        Assertions.assertTrue(service.fightAcceptedIfActive(EVENT_ID).isEmpty());

        assertNoFightOrWrites();
    }

    @ParameterizedTest
    @MethodSource("nonAcceptedPhases")
    void freshNonAcceptedPhaseDoesNotFight(Anomaly anomaly) {
        Mockito.when(events.getByIdForUpdate(EVENT_ID)).thenReturn(Optional.of(event(EventStatus.LAUNCHED)));
        Mockito.when(anomalies.findByLaunchedEventId(EVENT_ID)).thenReturn(Optional.of(anomaly));

        Assertions.assertTrue(service.fightAcceptedIfActive(EVENT_ID).isEmpty());

        assertNoFightOrWrites();
    }

    private static Stream<Anomaly> nonAcceptedPhases() {
        return Stream.of(
            new Anomaly.Safe(EVENT_ID, GROUP, OWNER, AnomalyPveTemplate.ASH_WIND, Anomaly.Safe.Phase.GATHERING),
            new Anomaly.Safe(EVENT_ID, GROUP, OWNER, AnomalyPveTemplate.ASH_WIND, Anomaly.Safe.Phase.PVE_WAITING),
            new Anomaly.Dangerous.Gathering(EVENT_ID, GROUP, OWNER),
            new Anomaly.Dangerous.Searching(EVENT_ID, GROUP, OWNER, 1000)
        );
    }

    @Test
    void acceptedAnomalyWithPersistedWinnerDoesNotFightAgain() {
        Mockito.when(events.getByIdForUpdate(EVENT_ID)).thenReturn(Optional.of(event(EventStatus.LAUNCHED)));
        Mockito.when(anomalies.findByLaunchedEventId(EVENT_ID)).thenReturn(Optional.of(accepted(true).withWinner(GROUP)));

        Assertions.assertTrue(service.fightAcceptedIfActive(EVENT_ID).isEmpty());

        assertNoFightOrWrites();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void activeAcceptedAnomalyUsesFreshStateAndSupportsHistoricMissingPartnerId(boolean linkedPartner) {
        final var current = event(EventStatus.LAUNCHED);
        final var accepted = accepted(linkedPartner);
        final var expected = new EventResult.AnomalyResult.BattleFinished(
            EVENT_ID, GROUP, OPPONENT, List.of(), List.of()
        );
        Mockito.when(events.getByIdForUpdate(EVENT_ID)).thenReturn(Optional.of(current));
        Mockito.when(anomalies.findByLaunchedEventId(EVENT_ID)).thenReturn(Optional.of(accepted));
        Mockito.doReturn(expected).when(service).fight(current, accepted);

        Assertions.assertSame(expected, service.fightAcceptedIfActive(EVENT_ID).orElseThrow());

        final var order = Mockito.inOrder(events, anomalies, service);
        order.verify(events).getByIdForUpdate(EVENT_ID);
        order.verify(anomalies).findByLaunchedEventId(EVENT_ID);
        order.verify(service).fight(current, accepted);
        Mockito.verify(events, Mockito.never()).getById(Mockito.anyLong());
    }

    @Test
    void fightFailurePropagatesInsteadOfBeingTreatedAsAlreadyFinished() {
        final var current = event(EventStatus.LAUNCHED);
        final var accepted = accepted(true);
        final var failure = new IllegalStateException("intentional completion failure");
        Mockito.when(events.getByIdForUpdate(EVENT_ID)).thenReturn(Optional.of(current));
        Mockito.when(anomalies.findByLaunchedEventId(EVENT_ID)).thenReturn(Optional.of(accepted));
        Mockito.doThrow(failure).when(service).fight(current, accepted);

        Assertions.assertSame(failure, Assertions.assertThrows(IllegalStateException.class,
            () -> service.fightAcceptedIfActive(EVENT_ID)));
    }

    private void assertNoFightOrWrites() {
        Mockito.verify(service, Mockito.never()).fight(Mockito.any(), Mockito.any());
        Mockito.verifyNoInteractions(participants, personages, loadouts, logs, config, ratings);
    }

    private static Anomaly.Dangerous.Accepted accepted(boolean linkedPartner) {
        return new Anomaly.Dangerous.Accepted(EVENT_ID, GROUP, OWNER, OPPONENT, OPPONENT_OWNER,
            linkedPartner ? Optional.of(131L) : Optional.empty(), Optional.empty(), 1000);
    }

    private static LaunchedEvent event(EventStatus status) {
        final var now = TimeUtils.moscowTime();
        // Matching starts the accepted fight immediately; it does not wait for the search deadline.
        return new LaunchedEvent(EVENT_ID, 5, now.minusHours(1), now.plusHours(1), status, Optional.empty());
    }
}
