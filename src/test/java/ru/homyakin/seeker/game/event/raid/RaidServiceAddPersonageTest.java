package ru.homyakin.seeker.game.event.raid;

import io.vavr.control.Either;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import ru.homyakin.seeker.game.event.raid.models.Raid;
import ru.homyakin.seeker.game.event.launched.LaunchedEventService;
import ru.homyakin.seeker.game.event.raid.models.RaidPersonageParams;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.event.raid.models.JoinToRaidResult;
import ru.homyakin.seeker.game.event.raid.models.AddPersonageToRaidError;
import ru.homyakin.seeker.game.personage.event.AddPersonageToEventRequest;
import ru.homyakin.seeker.game.personage.event.PersonageEventService;
import ru.homyakin.seeker.game.personage.event.RaidParticipant;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.models.errors.NotEnoughEnergy;
import ru.homyakin.seeker.test_utils.PersonageUtils;
import ru.homyakin.seeker.test_utils.event.EventUtils;
import ru.homyakin.seeker.test_utils.event.LaunchedEventUtils;
import ru.homyakin.seeker.utils.models.Success;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class RaidServiceAddPersonageTest {
    private final RaidDao raidDao = Mockito.mock();
    private final LaunchedEventService launchedEventService = Mockito.mock();
    private final PersonageService personageService = Mockito.mock();
    private final RaidConfig config = Mockito.mock();
    private final PersonageEventService personageEventService = Mockito.mock();
    private final RaidService service = new RaidService(
        raidDao,
        personageService,
        launchedEventService,
        personageEventService,
        config
    );
    private PersonageId personageId;
    private Raid raid;

    @BeforeEach
    public void init() {
        personageId = PersonageId.from(1L);
        raid = EventUtils.randomRaid();
        Mockito.when(config.energyCost()).thenReturn(33);
    }

    @Test
    public void Given_NotBusyPersonageAndWaitingEvent_When_JoinRaid_Then_ReturnJoinToRaidResultAndReducePersonageEnergy() {
        // given
        final var launchedEvent = LaunchedEventUtils.withEventId(1);
        Mockito.when(launchedEventService.getById(launchedEvent.id())).thenReturn(Optional.of(launchedEvent));
        Mockito.when(launchedEventService.getActiveEventByPersonageId(personageId)).thenReturn(Optional.empty());
        Mockito.when(personageEventService.addPersonageToLaunchedEvent(
            new AddPersonageToEventRequest(launchedEvent.id(), personageId, Optional.of(new RaidPersonageParams(false)))
            ))
            .thenReturn(Either.right(Success.INSTANCE));
        Mockito.when(raidDao.getByEventId(launchedEvent.eventId())).thenReturn(Optional.of(raid));
        final var participants = Stream.of(PersonageUtils.random(), PersonageUtils.withId(personageId))
            .map(it -> new RaidParticipant(it, new RaidPersonageParams(false)))
            .toList();
        Mockito.when(personageEventService.getRaidParticipants(launchedEvent.id())).thenReturn(participants);
        final var personage = PersonageUtils.withId(personageId);
        Mockito.when(personageService.checkPersonageEnergy(personageId, 33)).thenReturn(
            Either.right(personage)
        );
        Mockito.when(personageService.reduceEnergy(Mockito.eq(personage), Mockito.eq(33), Mockito.any())).thenReturn(
            Either.right(PersonageUtils.withId(personageId))
        );

        // when
        final var result = service.addPersonage(personageId, launchedEvent.id());

        // then
        final var expected = new JoinToRaidResult(
            launchedEvent,
            raid,
            participants,
            false
        );
        Assertions.assertTrue(result.isRight());
        Assertions.assertEquals(expected, result.get());

        final var captor = ArgumentCaptor.forClass(Integer.class);
        Mockito.verify(personageService).reduceEnergy(Mockito.any(), captor.capture(), Mockito.any());
        Assertions.assertEquals(33, captor.getValue());
    }

    @Test
    public void Given_LaunchedEventNotExist_When_JoinRaid_Then_ReturnRaidNotExistError() {
        // given
        final var launchedEvent = LaunchedEventUtils.withEventId(1);
        Mockito.when(launchedEventService.getById(launchedEvent.id())).thenReturn(Optional.empty());

        // when
        final var result = service.addPersonage(personageId, launchedEvent.id());

        // then
        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(AddPersonageToRaidError.RaidNotExist.INSTANCE, result.getLeft());
    }

    @Test
    public void Given_LaunchedEventIsFinished_When_JoinRaid_Then_ReturnEndedRaidError() {
        // given
        final var launchedEvent = LaunchedEventUtils.expiredWithEventId(1);
        Mockito.when(launchedEventService.getById(launchedEvent.id())).thenReturn(Optional.of(launchedEvent));
        Mockito.when(raidDao.getByEventId(launchedEvent.eventId())).thenReturn(Optional.of(raid));

        // when
        final var result = service.addPersonage(personageId, launchedEvent.id());

        // then
        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(AddPersonageToRaidError.RaidInFinalStatus.ExpiredRaid.INSTANCE, result.getLeft());
    }

    @Test
    public void Given_PersonageInThisEvent_When_JoinRaid_Then_ReturnPersonageInThisRaidError() {
        // given
        final var launchedEvent = LaunchedEventUtils.withEventId(1);
        Mockito.when(launchedEventService.getById(launchedEvent.id())).thenReturn(Optional.of(launchedEvent));
        Mockito.when(raidDao.getByEventId(launchedEvent.eventId())).thenReturn(Optional.of(raid));
        Mockito.when(launchedEventService.getActiveEventByPersonageId(personageId)).thenReturn(Optional.of(launchedEvent));

        // when
        final var result = service.addPersonage(personageId, launchedEvent.id());

        // then
        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(AddPersonageToRaidError.PersonageInThisRaid.INSTANCE, result.getLeft());
    }

    @Test
    public void Given_PersonageInOtherEvent_When_JoinRaid_Then_ReturnPersonageInOtherEventError() {
        // given
        final var launchedEvent = LaunchedEventUtils.withId(1);
        Mockito.when(launchedEventService.getById(launchedEvent.id())).thenReturn(Optional.of(launchedEvent));
        Mockito.when(raidDao.getByEventId(launchedEvent.eventId())).thenReturn(Optional.of(raid));
        Mockito.when(launchedEventService.getActiveEventByPersonageId(personageId))
            .thenReturn(Optional.of(LaunchedEventUtils.withId(2)));

        // when
        final var result = service.addPersonage(personageId, launchedEvent.id());

        // then
        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(AddPersonageToRaidError.PersonageInOtherEvent.INSTANCE, result.getLeft());
    }

    @Test
    public void Given_PersonageWithZeroEnergy_When_JoinRaid_Then_JoinExhausted() {
        // given
        final var launchedEvent = LaunchedEventUtils.withEventId(1);
        Mockito.when(launchedEventService.getById(launchedEvent.id())).thenReturn(Optional.of(launchedEvent));
        Mockito.when(raidDao.getByEventId(launchedEvent.eventId())).thenReturn(Optional.of(raid));
        Mockito.when(launchedEventService.getActiveEventByPersonageId(personageId)).thenReturn(Optional.empty());
        Mockito.when(personageService.checkPersonageEnergy(personageId, 33))
            .thenReturn(Either.left(NotEnoughEnergy.INSTANCE));
        Mockito.when(personageEventService.addPersonageToLaunchedEvent(
                new AddPersonageToEventRequest(launchedEvent.id(), personageId, Optional.of(new RaidPersonageParams(true)))
            ))
            .thenReturn(Either.right(Success.INSTANCE));
        final var participants = Stream.of(PersonageUtils.random(), PersonageUtils.withId(personageId))
            .map(it -> new RaidParticipant(it, new RaidPersonageParams(false)))
            .toList();
        Mockito.when(personageEventService.getRaidParticipants(launchedEvent.id())).thenReturn(participants);

        // when
        final var result = service.addPersonage(personageId, launchedEvent.id());

        // then
        final var expected = new JoinToRaidResult(
            launchedEvent,
            raid,
            participants,
            true
        );
        Assertions.assertTrue(result.isRight());
        Assertions.assertEquals(expected, result.get());

        Mockito.verify(personageService, Mockito.times(0))
            .reduceEnergy(Mockito.any(), Mockito.anyInt(), Mockito.any());
    }
}
