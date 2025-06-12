package ru.homyakin.seeker.game.event.personal_quest;

import io.vavr.control.Either;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.homyakin.seeker.game.event.launched.CurrentEvent;
import ru.homyakin.seeker.game.event.launched.CurrentEvents;
import ru.homyakin.seeker.game.event.models.EventStatus;
import ru.homyakin.seeker.game.event.launched.LaunchedEvent;
import ru.homyakin.seeker.game.event.models.EventType;
import ru.homyakin.seeker.game.event.personal_quest.model.PersonalQuest;
import ru.homyakin.seeker.game.event.personal_quest.model.PersonalQuestPersonageParams;
import ru.homyakin.seeker.game.event.personal_quest.model.TakeQuestError;
import ru.homyakin.seeker.game.event.personal_quest.model.StartedQuest;
import ru.homyakin.seeker.game.event.launched.LaunchedEventService;
import ru.homyakin.seeker.game.event.world_raid.action.WorldRaidContributionService;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.event.AddPersonageToEventRequest;
import ru.homyakin.seeker.game.personage.event.PersonageEventService;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.models.errors.NotEnoughEnergy;
import ru.homyakin.seeker.game.personage.notification.action.SendNotificationToPersonageCommand;
import ru.homyakin.seeker.game.stats.action.PersonageStatsService;
import ru.homyakin.seeker.infrastructure.lock.InMemoryLockService;
import ru.homyakin.seeker.infrastructure.lock.LockPrefixes;
import ru.homyakin.seeker.infrastructure.lock.LockService;
import ru.homyakin.seeker.test_utils.PersonageUtils;
import ru.homyakin.seeker.utils.TimeUtils;
import ru.homyakin.seeker.utils.models.Success;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class PersonalQuestServiceTakeQuestTest {
    private final PersonalQuestDao personalQuestDao = Mockito.mock();
    private final PersonageService personageService = Mockito.mock();
    private final LockService lockService = new InMemoryLockService();
    private final LaunchedEventService launchedEventService = Mockito.mock();
    private final PersonalQuestConfig config = Mockito.mock();
    private final PersonageEventService personageEventService = Mockito.mock();
    private final SendNotificationToPersonageCommand sendNotificationToPersonageCommand = Mockito.mock();
    private final WorldRaidContributionService worldRaidContributionService = Mockito.mock();
    private final PersonageStatsService personageStatsService = Mockito.mock();
    private final PersonalQuestService personalQuestService = new PersonalQuestService(
        personalQuestDao,
        personageService,
        lockService,
        launchedEventService,
        personageEventService,
        config,
        sendNotificationToPersonageCommand,
        worldRaidContributionService,
        personageStatsService
    );

    @BeforeEach
    public void init() {
        Mockito.when(config.requiredEnergy()).thenReturn(REQUIRED_ENERGY);
    }

    @Test
    public void Given_PersonageLocked_When_TakeQuest_Then_ReturnPersonageLocked() {
        // Given
        PersonageId personageId = new PersonageId(1);
        lockService.tryLock(LockPrefixes.PERSONAGE.name() + "-" + personageId.value());

        // When
        final var result = personalQuestService.takeQuest(personageId, 1);

        // Then
        assertEquals(Either.left(TakeQuestError.PersonageLocked.INSTANCE), result);
    }

    @Test
    public void Given_CountLessThanOne_When_TakeQuest_Then_ReturnNotPositiveCountError() {
        // Given
        PersonageId personageId = new PersonageId(1);

        // When
        final var result = personalQuestService.takeQuest(personageId, 0);

        // Then
        assertEquals(Either.left(TakeQuestError.NotPositiveCount.INSTANCE), result);
    }

    @Test
    public void Given_PersonageWithEnoughEnergy_When_TakeQuests_Then_ReturnStartedQuest() {
        // given
        final var personage = PersonageUtils.random();
        final var count = 3;

        // when
        when(personageService.checkPersonageEnergy(personage.id(), config.requiredEnergy() * count))
            .thenReturn(Either.right(personage));
        when(launchedEventService.getActiveEventsByPersonageId(personage.id())).thenReturn(new CurrentEvents(List.of()));
        when(personalQuestDao.getRandomQuest()).thenReturn(Optional.of(quest));
        when(launchedEventService.createFromPersonalQuest(eq(quest), any(), any())).thenReturn(launchedEvent);
        when(
            personageEventService.addPersonageToLaunchedEvent(
                new AddPersonageToEventRequest(
                    launchedEvent.id(),
                    personage.id(),
                    Optional.of(new PersonalQuestPersonageParams(count))
                )
            )
        ).thenReturn(Either.right(Success.INSTANCE));
        when(personageService.reduceEnergy(eq(personage), eq(REQUIRED_ENERGY * count), any()))
            .thenReturn(Either.right(personage));

        final var result = personalQuestService.takeQuest(personage.id(), count);

        // then
        final var expected = new StartedQuest.Multiple(
            count,
            config.requiredTime().multipliedBy(count),
            config.requiredEnergy() * count
        );
        assertTrue(result.isRight());
        assertEquals(expected, result.get());
    }

    @Test
    public void Given_PersonageWithEnoughEnergy_When_TakeQuest_Then_ReturnStartedQuest() {
        // given
        final var personage = PersonageUtils.random();

        // when
        when(personageService.checkPersonageEnergy(personage.id(), config.requiredEnergy()))
            .thenReturn(Either.right(personage));
        when(launchedEventService.getActiveEventsByPersonageId(personage.id())).thenReturn(new CurrentEvents(List.of()));
        when(personalQuestDao.getRandomQuest()).thenReturn(Optional.of(quest));
        when(launchedEventService.createFromPersonalQuest(eq(quest), any(), any())).thenReturn(launchedEvent);
        when(
            personageEventService.addPersonageToLaunchedEvent(
                new AddPersonageToEventRequest(
                    launchedEvent.id(),
                    personage.id(),
                    Optional.of(new PersonalQuestPersonageParams(1))
                )
            )
        ).thenReturn(Either.right(Success.INSTANCE));
        when(personageService.reduceEnergy(eq(personage), eq(REQUIRED_ENERGY), any())).thenReturn(Either.right(personage));

        final var result = personalQuestService.takeQuest(personage.id(), 1);

        // then
        final var expected = new StartedQuest.Single(quest, config.requiredTime(), config.requiredEnergy());
        assertTrue(result.isRight());
        assertEquals(expected, result.get());
    }

    @Test
    public void Given_PersonageWithNotEnoughEnergy_When_TakeQuest_Then_ReturnNotEnoughEnergy() {
        // given
        final var personageId = new PersonageId(1);
        when(personageService.checkPersonageEnergy(personageId, config.requiredEnergy())).thenReturn(Either.left(NotEnoughEnergy.INSTANCE));

        // when
        final var result = personalQuestService.takeQuest(personageId, 1);

        // then
        assertEquals(Either.left(new TakeQuestError.NotEnoughEnergy(REQUIRED_ENERGY)), result);
    }

    @Test
    public void Given_PersonageWithEnoughEnergy_When_LaunchedEventIsPresent_Then_ReturnPersonageInOtherEvent() {
        // given
        final var personage = PersonageUtils.random();
        when(personageService.checkPersonageEnergy(personage.id(), config.requiredEnergy())).thenReturn(Either.right(personage));
        when(launchedEventService.getActiveEventsByPersonageId(personage.id()))
            .thenReturn(
                new CurrentEvents(
                    List.of(
                        new CurrentEvent(launchedEvent.id(), EventType.PERSONAL_QUEST, TimeUtils.moscowTime())
                    )
                )
            );

        // when
        final var result = personalQuestService.takeQuest(personage.id(), 1);

        // then
        assertEquals(Either.left(TakeQuestError.PersonageInOtherEvent.INSTANCE), result);
    }

    @Test
    public void Given_PersonageWithEnoughEnergy_When_NoQuestsInDb_Then_ReturnNoQuests() {
        // Arrange
        final var personageId = new PersonageId(1);
        when(personageService.checkPersonageEnergy(personageId, config.requiredEnergy())).thenReturn(Either.right(Mockito.mock()));
        when(launchedEventService.getActiveEventsByPersonageId(personageId)).thenReturn(new CurrentEvents(List.of()));
        when(personalQuestDao.getRandomQuest()).thenReturn(Optional.empty());

        // Act
        final var result = personalQuestService.takeQuest(personageId, 1);

        // Assert
        assertEquals(Either.left(TakeQuestError.NoQuests.INSTANCE), result);
    }

    private final PersonalQuest quest = new PersonalQuest(
        1,
        "",
        Collections.emptyMap()
    );
    private final LaunchedEvent launchedEvent = new LaunchedEvent(
        1,
        quest.eventId(),
        TimeUtils.moscowTime(),
        TimeUtils.moscowTime().plusMinutes(5),
        EventStatus.LAUNCHED
    );
    private final int REQUIRED_ENERGY = 20;
}
