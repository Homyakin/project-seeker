package ru.homyakin.seeker.game.personage;

import io.vavr.control.Either;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.homyakin.seeker.game.event.raid.RaidService;
import ru.homyakin.seeker.game.event.raid.models.Raid;
import ru.homyakin.seeker.game.event.service.LaunchedEventService;
import ru.homyakin.seeker.game.personage.badge.BadgeService;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.errors.PersonageEventError;
import ru.homyakin.seeker.test_utils.PersonageUtils;
import ru.homyakin.seeker.test_utils.event.EventUtils;
import ru.homyakin.seeker.test_utils.event.LaunchedEventUtils;
import ru.homyakin.seeker.utils.models.Success;

import java.time.Duration;
import java.util.Optional;

public class PersonageServiceAddEventTest {
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
    public void Given_NotBusyPersonageAndWaitingEvent_When_AddEvent_Then_ReturnLaunchedEvent() {
        // given
        final var launchedEvent = LaunchedEventUtils.withEventId(1);
        Mockito.when(launchedEventService.getById(launchedEvent.id())).thenReturn(Optional.of(launchedEvent));
        Mockito.when(launchedEventService.getActiveEventByPersonageId(personage.id())).thenReturn(Optional.empty());
        Mockito.when(launchedEventService.addPersonageToLaunchedEvent(personage.id(), launchedEvent.id()))
            .thenReturn(Either.right(Success.INSTANCE));
        Mockito.when(personageDao.getById(personage.id())).thenReturn(Optional.of(personage));

        // when
        final var result = service.addEvent(personage.id(), launchedEvent.id());

        // then
        Assertions.assertTrue(result.isRight());
        Assertions.assertEquals(launchedEvent, result.get());
    }

    @Test
    public void Given_LaunchedEventNotExist_When_AddEvent_Then_ReturnEventNotExistError() {
        // given
        final var launchedEvent = LaunchedEventUtils.withEventId(1);
        Mockito.when(launchedEventService.getById(launchedEvent.id())).thenReturn(Optional.empty());

        // when
        final var result = service.addEvent(personage.id(), launchedEvent.id());

        // then
        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(PersonageEventError.EventNotExist.INSTANCE, result.getLeft());
    }

    @Test
    public void Given_LaunchedEventIsFinished_When_AddEvent_Then_ReturnExpiredEventError() {
        // given
        final var launchedEvent = LaunchedEventUtils.expiredWithEventId(1);
        Mockito.when(launchedEventService.getById(launchedEvent.id())).thenReturn(Optional.of(launchedEvent));
        Mockito.when(raidService.getByEventId(launchedEvent.eventId())).thenReturn(Optional.of(raid));

        // when
        final var result = service.addEvent(personage.id(), launchedEvent.id());

        // then
        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(new PersonageEventError.ExpiredEvent(raid), result.getLeft());
    }

    @Test
    public void Given_PersonageInThisEvent_When_AddEvent_Then_ReturnPersonageInThisEventError() {
        // given
        final var launchedEvent = LaunchedEventUtils.withEventId(1);
        Mockito.when(launchedEventService.getById(launchedEvent.id())).thenReturn(Optional.of(launchedEvent));
        Mockito.when(raidService.getByEventId(launchedEvent.eventId())).thenReturn(Optional.of(raid));
        Mockito.when(launchedEventService.getActiveEventByPersonageId(personage.id())).thenReturn(Optional.of(launchedEvent));
        Mockito.when(personageDao.getById(personage.id())).thenReturn(Optional.of(personage));

        // when
        final var result = service.addEvent(personage.id(), launchedEvent.id());

        // then
        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(PersonageEventError.PersonageInThisEvent.INSTANCE, result.getLeft());
    }

    @Test
    public void Given_PersonageInOtherEvent_When_AddEvent_Then_ReturnPersonageInThisEventError() {
        // given
        final var launchedEvent = LaunchedEventUtils.withEventId(1);
        Mockito.when(launchedEventService.getById(launchedEvent.id())).thenReturn(Optional.of(launchedEvent));
        Mockito.when(raidService.getByEventId(launchedEvent.eventId())).thenReturn(Optional.of(raid));
        Mockito.when(launchedEventService.getActiveEventByPersonageId(personage.id())).thenReturn(Optional.of(LaunchedEventUtils.withEventId(1)));
        Mockito.when(personageDao.getById(personage.id())).thenReturn(Optional.of(personage));

        // when
        final var result = service.addEvent(personage.id(), launchedEvent.id());

        // then
        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(PersonageEventError.PersonageInOtherEvent.INSTANCE, result.getLeft());
    }

    @Test
    public void Given_PersonageWithZeroEnergy_When_AddEvent_Then_ReturnNotEnoughEnergyError() {
        // given
        final var launchedEvent = LaunchedEventUtils.withEventId(1);
        personage = PersonageUtils.randomZeroEnergy();
        Mockito.when(launchedEventService.getById(launchedEvent.id())).thenReturn(Optional.of(launchedEvent));
        Mockito.when(raidService.getByEventId(launchedEvent.eventId())).thenReturn(Optional.of(raid));
        Mockito.when(launchedEventService.getActiveEventByPersonageId(personage.id())).thenReturn(Optional.empty());
        Mockito.when(personageDao.getById(personage.id())).thenReturn(Optional.of(personage));

        // when
        final var result = service.addEvent(personage.id(), launchedEvent.id());

        // then
        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(new PersonageEventError.NotEnoughEnergy(33), result.getLeft());
    }
}
