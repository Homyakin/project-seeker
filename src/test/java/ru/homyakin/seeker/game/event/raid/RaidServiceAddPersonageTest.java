package ru.homyakin.seeker.game.event.raid;

import io.vavr.control.Either;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import ru.homyakin.seeker.game.event.raid.models.Raid;
import ru.homyakin.seeker.game.event.service.LaunchedEventService;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.event.raid.models.JoinToRaidResult;
import ru.homyakin.seeker.game.event.raid.models.AddPersonageToRaidError;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.models.errors.NotEnoughEnergy;
import ru.homyakin.seeker.test_utils.PersonageUtils;
import ru.homyakin.seeker.test_utils.event.EventUtils;
import ru.homyakin.seeker.test_utils.event.LaunchedEventUtils;
import ru.homyakin.seeker.utils.models.Success;

import java.util.List;
import java.util.Optional;

public class RaidServiceAddPersonageTest {
    private final RaidDao raidDao = Mockito.mock();
    private final LaunchedEventService launchedEventService = Mockito.mock();
    private final PersonageService personageService = Mockito.mock();
    private final RaidConfig config = Mockito.mock();
    private final RaidService service = new RaidService(
        raidDao,
        personageService,
        launchedEventService,
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
        Mockito.when(launchedEventService.addPersonageToLaunchedEvent(personageId, launchedEvent.id()))
            .thenReturn(Either.right(Success.INSTANCE));
        Mockito.when(raidDao.getByEventId(launchedEvent.eventId())).thenReturn(Optional.of(raid));
        final var personages = List.of(PersonageUtils.random(), PersonageUtils.withId(personageId));
        Mockito.when(personageService.getByLaunchedEvent(launchedEvent.id())).thenReturn(personages);
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
            personages
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
    public void Given_PersonageWithZeroEnergy_When_JoinRaid_Then_ReturnNotEnoughEnergyError() {
        // given
        final var launchedEvent = LaunchedEventUtils.withEventId(1);
        Mockito.when(launchedEventService.getById(launchedEvent.id())).thenReturn(Optional.of(launchedEvent));
        Mockito.when(raidDao.getByEventId(launchedEvent.eventId())).thenReturn(Optional.of(raid));
        Mockito.when(launchedEventService.getActiveEventByPersonageId(personageId)).thenReturn(Optional.empty());
        Mockito.when(personageService.checkPersonageEnergy(personageId, 33))
            .thenReturn(Either.left(NotEnoughEnergy.INSTANCE));

        // when
        final var result = service.addPersonage(personageId, launchedEvent.id());

        // then
        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(new AddPersonageToRaidError.NotEnoughEnergy(33), result.getLeft());
    }
}
