package ru.homyakin.seeker.game.event.personal_quest;

import io.vavr.control.Either;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.homyakin.seeker.game.event.models.EventStatus;
import ru.homyakin.seeker.game.event.models.LaunchedEvent;
import ru.homyakin.seeker.game.event.personal_quest.model.PersonalQuest;
import ru.homyakin.seeker.game.event.personal_quest.model.TakeQuestError;
import ru.homyakin.seeker.game.event.personal_quest.model.StartedQuest;
import ru.homyakin.seeker.game.event.service.LaunchedEventService;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.models.errors.NotEnoughEnergy;
import ru.homyakin.seeker.infrastructure.lock.InMemoryLockService;
import ru.homyakin.seeker.infrastructure.lock.LockPrefixes;
import ru.homyakin.seeker.infrastructure.lock.LockService;
import ru.homyakin.seeker.test_utils.PersonageUtils;
import ru.homyakin.seeker.utils.TimeUtils;
import ru.homyakin.seeker.utils.models.Success;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class PersonalQuestServiceTakeQuestTest {
    private final PersonalQuestDao personalQuestDao = Mockito.mock(PersonalQuestDao.class);
    private final PersonageService personageService = Mockito.mock(PersonageService.class);
    private final LockService lockService = new InMemoryLockService();
    private final LaunchedEventService launchedEventService = Mockito.mock(LaunchedEventService.class);
    private final PersonalQuestConfig config = Mockito.mock(PersonalQuestConfig.class);
    private final PersonalQuestService personalQuestService = new PersonalQuestService(
        personalQuestDao,
        personageService,
        lockService,
        launchedEventService,
        config
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
        final var result = personalQuestService.takeQuest(personageId);

        // Then
        assertEquals(Either.left(TakeQuestError.PersonageLocked.INSTANCE), result);
    }

    @Test
    public void Given_PersonageWithEnoughEnergy_When_TakeQuest_Then_ReturnStartedQuest() {
        // given
        final var personage = PersonageUtils.random();

        // when
        when(personageService.checkPersonageEnergy(personage.id(), config.requiredEnergy()))
            .thenReturn(Either.right(personage));
        when(launchedEventService.getActiveEventByPersonageId(personage.id())).thenReturn(Optional.empty());
        when(personalQuestDao.getRandomQuest()).thenReturn(Optional.of(quest));
        when(launchedEventService.createFromPersonalQuest(eq(quest), any(), any())).thenReturn(launchedEvent);
        when(launchedEventService.addPersonageToLaunchedEvent(personage.id(), launchedEvent.id())).thenReturn(Either.right(Success.INSTANCE));
        when(personageService.reduceEnergy(eq(personage), eq(REQUIRED_ENERGY), any())).thenReturn(Either.right(personage));

        final var result = personalQuestService.takeQuest(personage.id());

        // then
        final var expected = new StartedQuest(quest, config.requiredTime());
        assertTrue(result.isRight());
        assertEquals(expected, result.get());
    }

    @Test
    public void Given_PersonageWithNotEnoughEnergy_When_TakeQuest_Then_ReturnNotEnoughEnergy() {
        // given
        final var personageId = new PersonageId(1);
        when(personageService.checkPersonageEnergy(personageId, config.requiredEnergy())).thenReturn(Either.left(NotEnoughEnergy.INSTANCE));

        // when
        final var result = personalQuestService.takeQuest(personageId);

        // then
        assertEquals(Either.left(new TakeQuestError.NotEnoughEnergy(REQUIRED_ENERGY)), result);
    }

    @Test
    public void Given_PersonageWithEnoughEnergy_When_LaunchedEventIsPresent_Then_ReturnPersonageInOtherEvent() {
        // given
        final var personage = PersonageUtils.random();
        when(personageService.checkPersonageEnergy(personage.id(), config.requiredEnergy())).thenReturn(Either.right(personage));
        when(launchedEventService.getActiveEventByPersonageId(personage.id())).thenReturn(Optional.of(launchedEvent));

        // when
        final var result = personalQuestService.takeQuest(personage.id());

        // then
        assertEquals(Either.left(TakeQuestError.PersonageInOtherEvent.INSTANCE), result);
    }

    @Test
    public void Given_PersonageWithEnoughEnergy_When_NoQuestsInDb_Then_ReturnNoQuests() {
        // Arrange
        final var personageId = new PersonageId(1);
        when(personageService.checkPersonageEnergy(personageId, config.requiredEnergy())).thenReturn(Either.right(Mockito.mock()));
        when(launchedEventService.getActiveEventByPersonageId(personageId)).thenReturn(Optional.empty());
        when(personalQuestDao.getRandomQuest()).thenReturn(Optional.empty());

        // Act
        final var result = personalQuestService.takeQuest(personageId);

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
