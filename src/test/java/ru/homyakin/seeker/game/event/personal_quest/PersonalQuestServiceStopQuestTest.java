package ru.homyakin.seeker.game.event.personal_quest;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.homyakin.seeker.game.event.models.EventResult;
import ru.homyakin.seeker.game.event.launched.LaunchedEvent;
import ru.homyakin.seeker.game.event.personal_quest.model.PersonalQuest;
import ru.homyakin.seeker.game.event.launched.LaunchedEventService;
import ru.homyakin.seeker.game.event.personal_quest.model.PersonalQuestPersonageParams;
import ru.homyakin.seeker.game.event.personal_quest.model.PersonalQuestResult;
import ru.homyakin.seeker.game.event.world_raid.action.WorldRaidContributionService;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.event.PersonageEventService;
import ru.homyakin.seeker.game.personage.event.QuestParticipant;
import ru.homyakin.seeker.game.personage.notification.action.SendNotificationToPersonageCommand;
import ru.homyakin.seeker.game.personage.notification.entity.Notification;
import ru.homyakin.seeker.infrastructure.lock.InMemoryLockService;
import ru.homyakin.seeker.infrastructure.lock.LockService;
import ru.homyakin.seeker.test_utils.PersonageUtils;
import ru.homyakin.seeker.test_utils.event.LaunchedEventUtils;
import ru.homyakin.seeker.utils.RandomUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

public class PersonalQuestServiceStopQuestTest {
    private final PersonalQuestDao personalQuestDao = Mockito.mock();
    private final PersonageService personageService = Mockito.mock();
    private final LockService lockService = new InMemoryLockService();
    private final LaunchedEventService launchedEventService = Mockito.mock();
    private final PersonalQuestConfig config = Mockito.mock();
    private final PersonageEventService personageEventService = Mockito.mock();
    private final SendNotificationToPersonageCommand sendNotificationToPersonageCommand = Mockito.mock();
    private final WorldRaidContributionService worldRaidContributionService = Mockito.mock();
    private final PersonalQuestService personalQuestService = new PersonalQuestService(
        personalQuestDao,
        personageService,
        lockService,
        launchedEventService,
        personageEventService,
        config,
        sendNotificationToPersonageCommand,
        worldRaidContributionService
    );

    @Test
    public void Given_LaunchedEvent_When_RandomIsSuccess_Then_ReturnSuccessAndNotify() {
        // given
        final var participant = new QuestParticipant(PersonageUtils.random(), new PersonalQuestPersonageParams(1));

        // when
        final EventResult.PersonalQuestEventResult result;
        try (final var mock = Mockito.mockStatic(RandomUtils.class)) {
            mock.when(() -> RandomUtils.processChance(config.baseSuccessProbability())).thenReturn(true);
            mock.when(() -> RandomUtils.getInInterval(config.reward())).thenReturn(REWARD.value());
            when(personalQuestDao.getByEventId(launchedEvent.eventId())).thenReturn(Optional.of(quest));
            when(personageEventService.getQuestParticipants(launchedEvent.id())).thenReturn(List.of(participant));

            result = personalQuestService.stopQuest(launchedEvent);
        }

        // then
        final var expected = new EventResult.PersonalQuestEventResult.Single(
            quest,
            participant.personage(),
            new PersonalQuestResult.Success(REWARD)
        );
        assertEquals(expected, result);
        Mockito.verify(sendNotificationToPersonageCommand)
            .sendNotification(participant.personage().id(), new Notification.QuestResult(expected));
        Mockito.verify(worldRaidContributionService).questComplete(participant.personage().id());
    }

    @Test
    public void Given_LaunchedEvent_When_RandomIsNotSuccess_Then_ReturnFailureAndNotify() {
        // given
        final var participant = new QuestParticipant(PersonageUtils.random(), new PersonalQuestPersonageParams(1));

        // when
        final EventResult.PersonalQuestEventResult result;
        try (final var mock = Mockito.mockStatic(RandomUtils.class)) {
            mock.when(() -> RandomUtils.processChance(config.baseSuccessProbability())).thenReturn(false);
            mock.when(() -> RandomUtils.getInInterval(config.reward())).thenReturn(REWARD.value());
            when(personalQuestDao.getByEventId(launchedEvent.eventId())).thenReturn(Optional.of(quest));
            when(personageEventService.getQuestParticipants(launchedEvent.id())).thenReturn(List.of(participant));

            result = personalQuestService.stopQuest(launchedEvent);
        }

        // then
        final var expected = new EventResult.PersonalQuestEventResult.Single(
            quest,
            participant.personage(),
            PersonalQuestResult.Failure.INSTANCE
        );
        assertEquals(expected, result);
        Mockito.verify(sendNotificationToPersonageCommand).sendNotification(
            participant.personage().id(), new Notification.QuestResult(expected)
        );
        Mockito.verify(worldRaidContributionService).questComplete(participant.personage().id());
    }

    @Test
    public void Given_QuestCountMoreThanOne_Then_ReturnMultipleResult() {
        // given
        final var participant = new QuestParticipant(PersonageUtils.random(), new PersonalQuestPersonageParams(3));

        // when
        final EventResult.PersonalQuestEventResult result;
        try (final var mock = Mockito.mockStatic(RandomUtils.class)) {
            mock.when(() -> RandomUtils.processChance(anyInt()))
                .thenReturn(true)
                .thenReturn(false)
                .thenReturn(true);
            mock.when(() -> RandomUtils.getInInterval(config.reward())).thenReturn(REWARD.value());
            when(personalQuestDao.getByEventId(launchedEvent.eventId())).thenReturn(Optional.of(quest));
            when(personageEventService.getQuestParticipants(launchedEvent.id())).thenReturn(List.of(participant));

            result = personalQuestService.stopQuest(launchedEvent);
        }

        // then
        final var expected = new EventResult.PersonalQuestEventResult.Multiple(
            participant.personage(),
            List.of(
                new PersonalQuestResult.Success(REWARD),
                PersonalQuestResult.Failure.INSTANCE,
                new PersonalQuestResult.Success(REWARD)
            )
        );
        assertEquals(expected, result);
        Mockito.verify(sendNotificationToPersonageCommand).sendNotification(
            participant.personage().id(), new Notification.QuestResult(expected)
        );
        Mockito.verify(launchedEventService).updateResult(launchedEvent, expected.results().getFirst());
        Mockito.verify(launchedEventService).createFinished(eq(quest), any(), eq(expected.results().get(1)));
        Mockito.verify(launchedEventService).createFinished(eq(quest), any(), eq(expected.results().get(2)));
        Mockito.verify(launchedEventService, times(2)).createFinished(any(), any(), any());
        Mockito.verify(worldRaidContributionService).questComplete(participant.personage().id(), 3);
    }

    @Test
    public void When_EventIsNotQuest_Then_ThrowException() {
        // when
        when(personalQuestDao.getByEventId(launchedEvent.eventId())).thenReturn(Optional.empty());
        final var exception = assertThrows(
            IllegalStateException.class,
            () -> personalQuestService.stopQuest(launchedEvent)
        );

        // then
        assertEquals("Event " + launchedEvent.eventId() + " is not quest", exception.getMessage());
        Mockito.verify(worldRaidContributionService, times(0)).questComplete(any());
    }

    @Test
    public void When_PersonagesCountIsNotOne_Then_ThrowException() {
        // given
        final var participant1 = new QuestParticipant(PersonageUtils.random(), new PersonalQuestPersonageParams(1));
        final var participant2 = new QuestParticipant(PersonageUtils.random(), new PersonalQuestPersonageParams(1));

        when(personalQuestDao.getByEventId(launchedEvent.eventId())).thenReturn(Optional.of(quest));
        when(personageEventService.getQuestParticipants(launchedEvent.id())).thenReturn(List.of(participant1, participant2));

        assertThrows(IllegalStateException.class, () -> personalQuestService.stopQuest(launchedEvent));

        Mockito.verify(launchedEventService).setError(any());
        Mockito.verify(worldRaidContributionService, times(0)).questComplete(any());
    }

    @Test
    public void When_QuestCountIsLessThanOne_Then_ThrowException() {
        // given
        final var participant = new QuestParticipant(PersonageUtils.random(), new PersonalQuestPersonageParams(0));

        when(personalQuestDao.getByEventId(launchedEvent.eventId())).thenReturn(Optional.of(quest));
        when(personageEventService.getQuestParticipants(launchedEvent.id())).thenReturn(List.of(participant));

        assertThrows(IllegalStateException.class, () -> personalQuestService.stopQuest(launchedEvent));

        Mockito.verify(launchedEventService).setError(any());
        Mockito.verify(worldRaidContributionService, times(0)).questComplete(any());
    }

    private final LaunchedEvent launchedEvent = LaunchedEventUtils.withEventId(1);
    private final PersonalQuest quest = new PersonalQuest(launchedEvent.eventId(), "Quest Name", Collections.emptyMap());
    private final Money REWARD = new Money(1000);
}
