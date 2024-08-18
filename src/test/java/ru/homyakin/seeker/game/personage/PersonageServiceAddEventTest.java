package ru.homyakin.seeker.game.personage;

import io.vavr.control.Either;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.homyakin.seeker.game.event.models.Event;
import ru.homyakin.seeker.game.event.service.EventService;
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
    private final EventService eventService = Mockito.mock(EventService.class);
    private final PersonageConfig config = Mockito.mock(PersonageConfig.class);
    private final PersonageService service = new PersonageService(
        personageDao,
        launchedEventService,
        Mockito.mock(PersonageRaidResultDao.class),
        eventService,
        Mockito.mock(BadgeService.class),
        config
    );
    private Personage personage;
    private Event event;

    @BeforeEach
    public void init() {
        personage = PersonageUtils.random();
        event = EventUtils.randomEvent();
        Mockito.when(config.raidEnergyCost()).thenReturn(33);
        Mockito.when(config.energyFullRecovery()).thenReturn(Duration.ofHours(100));
    }

    @Test
    public void Given_NotBusyPersonageAndWaitingEvent_When_AddEvent_Then_ReturnLaunchedEvent() {
        // given
        final var launchedEvent = LaunchedEventUtils.fromEvent(event);
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
        final var launchedEvent = LaunchedEventUtils.fromEvent(event);
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
        final var launchedEvent = LaunchedEventUtils.expiredFromEvent(event);
        Mockito.when(launchedEventService.getById(launchedEvent.id())).thenReturn(Optional.of(launchedEvent));
        Mockito.when(eventService.getEventById(launchedEvent.eventId())).thenReturn(Optional.of(event));

        // when
        final var result = service.addEvent(personage.id(), launchedEvent.id());

        // then
        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(new PersonageEventError.ExpiredEvent(event), result.getLeft());
    }

    @Test
    public void Given_PersonageInThisEvent_When_AddEvent_Then_ReturnPersonageInThisEventError() {
        // given
        final var launchedEvent = LaunchedEventUtils.fromEvent(event);
        Mockito.when(launchedEventService.getById(launchedEvent.id())).thenReturn(Optional.of(launchedEvent));
        Mockito.when(eventService.getEventById(launchedEvent.eventId())).thenReturn(Optional.of(event));
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
        final var launchedEvent = LaunchedEventUtils.fromEvent(event);
        Mockito.when(launchedEventService.getById(launchedEvent.id())).thenReturn(Optional.of(launchedEvent));
        Mockito.when(eventService.getEventById(launchedEvent.eventId())).thenReturn(Optional.of(event));
        Mockito.when(launchedEventService.getActiveEventByPersonageId(personage.id())).thenReturn(Optional.of(LaunchedEventUtils.fromEvent(event)));
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
        final var launchedEvent = LaunchedEventUtils.fromEvent(event);
        personage = PersonageUtils.randomZeroEnergy();
        Mockito.when(launchedEventService.getById(launchedEvent.id())).thenReturn(Optional.of(launchedEvent));
        Mockito.when(eventService.getEventById(launchedEvent.eventId())).thenReturn(Optional.of(event));
        Mockito.when(launchedEventService.getActiveEventByPersonageId(personage.id())).thenReturn(Optional.empty());
        Mockito.when(personageDao.getById(personage.id())).thenReturn(Optional.of(personage));

        // when
        final var result = service.addEvent(personage.id(), launchedEvent.id());

        // then
        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(new PersonageEventError.NotEnoughEnergy(33), result.getLeft());
    }
}
