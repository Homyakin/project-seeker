package ru.homyakin.seeker.game.personage;

import io.vavr.control.Either;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import ru.homyakin.seeker.game.event.raid.RaidService;
import ru.homyakin.seeker.game.event.raid.models.Raid;
import ru.homyakin.seeker.game.event.service.LaunchedEventService;
import ru.homyakin.seeker.game.personage.badge.BadgeService;
import ru.homyakin.seeker.game.personage.models.JoinToRaidResult;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.errors.AddPersonageToRaidError;
import ru.homyakin.seeker.test_utils.PersonageUtils;
import ru.homyakin.seeker.test_utils.event.EventUtils;
import ru.homyakin.seeker.test_utils.event.LaunchedEventUtils;
import ru.homyakin.seeker.utils.models.Success;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public class PersonageServiceJoinRaidTest {
    private final PersonageDao personageDao = Mockito.mock(PersonageDao.class);
    private final LaunchedEventService launchedEventService = Mockito.mock(LaunchedEventService.class);
    private final RaidService raidService = Mockito.mock(RaidService.class);
    private final PersonageConfig config = Mockito.mock(PersonageConfig.class);
    private final PersonageService service = new PersonageService(
        personageDao,
        launchedEventService,
        Mockito.mock(PersonageRaidResultDao.class),
        raidService,
        Mockito.mock(BadgeService.class),
        config
    );
    private Personage personage;
    private Raid raid;

    @BeforeEach
    public void init() {
        personage = PersonageUtils.random();
        raid = EventUtils.randomRaid();
        Mockito.when(config.raidEnergyCost()).thenReturn(33);
        Mockito.when(config.energyFullRecovery()).thenReturn(Duration.ofHours(100));
    }

    @Test
    public void Given_NotBusyPersonageAndWaitingEvent_When_JoinRaid_Then_ReturnJoinToRaidResultAndReducePersonageEnergy() {
        // given
        final var launchedEvent = LaunchedEventUtils.withEventId(1);
        Mockito.when(launchedEventService.getById(launchedEvent.id())).thenReturn(Optional.of(launchedEvent));
        Mockito.when(launchedEventService.getActiveEventByPersonageId(personage.id())).thenReturn(Optional.empty());
        Mockito.when(launchedEventService.addPersonageToLaunchedEvent(personage.id(), launchedEvent.id()))
            .thenReturn(Either.right(Success.INSTANCE));
        Mockito.when(personageDao.getById(personage.id())).thenReturn(Optional.of(personage));
        Mockito.when(raidService.getByEventId(launchedEvent.eventId())).thenReturn(Optional.of(raid));
        final var personages = List.of(PersonageUtils.random(), PersonageUtils.withId(personage.id()));
        Mockito.when(personageDao.getByLaunchedEvent(launchedEvent.id())).thenReturn(personages);

        // when
        final var result = service.joinRaid(personage.id(), launchedEvent.id());

        // then
        final var expected = new JoinToRaidResult(
            launchedEvent,
            raid,
            personages
        );
        Assertions.assertTrue(result.isRight());
        Assertions.assertEquals(expected, result.get());

        final var captor = ArgumentCaptor.forClass(Personage.class);
        Mockito.verify(personageDao).update(captor.capture());
        Assertions.assertEquals(67, captor.getValue().energy().value());
    }

    @Test
    public void Given_LaunchedEventNotExist_When_JoinRaid_Then_ReturnRaidNotExistError() {
        // given
        final var launchedEvent = LaunchedEventUtils.withEventId(1);
        Mockito.when(launchedEventService.getById(launchedEvent.id())).thenReturn(Optional.empty());

        // when
        final var result = service.joinRaid(personage.id(), launchedEvent.id());

        // then
        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(AddPersonageToRaidError.RaidNotExist.INSTANCE, result.getLeft());
    }

    @Test
    public void Given_LaunchedEventIsFinished_When_JoinRaid_Then_ReturnEndedRaidError() {
        // given
        final var launchedEvent = LaunchedEventUtils.expiredWithEventId(1);
        Mockito.when(launchedEventService.getById(launchedEvent.id())).thenReturn(Optional.of(launchedEvent));
        Mockito.when(raidService.getByEventId(launchedEvent.eventId())).thenReturn(Optional.of(raid));

        // when
        final var result = service.joinRaid(personage.id(), launchedEvent.id());

        // then
        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(new AddPersonageToRaidError.EndedRaid(launchedEvent, raid), result.getLeft());
    }

    @Test
    public void Given_PersonageInThisEvent_When_JoinRaid_Then_ReturnPersonageInThisRaidError() {
        // given
        final var launchedEvent = LaunchedEventUtils.withEventId(1);
        Mockito.when(launchedEventService.getById(launchedEvent.id())).thenReturn(Optional.of(launchedEvent));
        Mockito.when(raidService.getByEventId(launchedEvent.eventId())).thenReturn(Optional.of(raid));
        Mockito.when(launchedEventService.getActiveEventByPersonageId(personage.id())).thenReturn(Optional.of(launchedEvent));
        Mockito.when(personageDao.getById(personage.id())).thenReturn(Optional.of(personage));

        // when
        final var result = service.joinRaid(personage.id(), launchedEvent.id());

        // then
        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(AddPersonageToRaidError.PersonageInThisRaid.INSTANCE, result.getLeft());
    }

    @Test
    public void Given_PersonageInOtherEvent_When_JoinRaid_Then_ReturnPersonageInOtherEventError() {
        // given
        final var launchedEvent = LaunchedEventUtils.withId(1);
        Mockito.when(launchedEventService.getById(launchedEvent.id())).thenReturn(Optional.of(launchedEvent));
        Mockito.when(raidService.getByEventId(launchedEvent.eventId())).thenReturn(Optional.of(raid));
        Mockito.when(launchedEventService.getActiveEventByPersonageId(personage.id()))
            .thenReturn(Optional.of(LaunchedEventUtils.withId(2)));
        Mockito.when(personageDao.getById(personage.id())).thenReturn(Optional.of(personage));

        // when
        final var result = service.joinRaid(personage.id(), launchedEvent.id());

        // then
        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(AddPersonageToRaidError.PersonageInOtherEvent.INSTANCE, result.getLeft());
    }

    @Test
    public void Given_PersonageWithZeroEnergy_When_JoinRaid_Then_ReturnNotEnoughEnergyError() {
        // given
        final var launchedEvent = LaunchedEventUtils.withEventId(1);
        personage = PersonageUtils.randomZeroEnergy(config.energyFullRecovery());
        Mockito.when(launchedEventService.getById(launchedEvent.id())).thenReturn(Optional.of(launchedEvent));
        Mockito.when(raidService.getByEventId(launchedEvent.eventId())).thenReturn(Optional.of(raid));
        Mockito.when(launchedEventService.getActiveEventByPersonageId(personage.id())).thenReturn(Optional.empty());
        Mockito.when(personageDao.getById(personage.id())).thenReturn(Optional.of(personage));

        // when
        final var result = service.joinRaid(personage.id(), launchedEvent.id());

        // then
        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(new AddPersonageToRaidError.NotEnoughEnergy(33), result.getLeft());
    }
}
