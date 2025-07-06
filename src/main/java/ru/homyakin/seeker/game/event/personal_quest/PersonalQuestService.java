package ru.homyakin.seeker.game.event.personal_quest;

import io.vavr.control.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.homyakin.seeker.game.event.models.EventResult;
import ru.homyakin.seeker.game.event.launched.LaunchedEvent;
import ru.homyakin.seeker.game.event.personal_quest.model.PersonalQuestPersonageParams;
import ru.homyakin.seeker.game.event.personal_quest.model.PersonalQuestRequirements;
import ru.homyakin.seeker.game.event.personal_quest.model.PersonalQuestResult;
import ru.homyakin.seeker.game.event.personal_quest.model.StartedQuest;
import ru.homyakin.seeker.game.event.personal_quest.model.TakeQuestError;
import ru.homyakin.seeker.game.event.launched.LaunchedEventService;
import ru.homyakin.seeker.game.event.world_raid.action.WorldRaidContributionService;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.event.AddPersonageToEventRequest;
import ru.homyakin.seeker.game.personage.event.PersonageEventService;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.notification.action.SendNotificationToPersonageCommand;
import ru.homyakin.seeker.game.personage.notification.entity.Notification;
import ru.homyakin.seeker.game.stats.action.PersonageStatsService;
import ru.homyakin.seeker.infrastructure.init.saving_models.SavingPersonalQuest;
import ru.homyakin.seeker.infrastructure.lock.LockPrefixes;
import ru.homyakin.seeker.infrastructure.lock.LockService;
import ru.homyakin.seeker.utils.MathUtils;
import ru.homyakin.seeker.utils.RandomUtils;
import ru.homyakin.seeker.utils.TimeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PersonalQuestService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final PersonalQuestDao personalQuestDao;
    private final PersonageService personageService;
    private final LockService lockService;
    private final LaunchedEventService launchedEventService;
    private final PersonalQuestConfig config;
    private final PersonageEventService personageEventService;
    private final SendNotificationToPersonageCommand sendNotificationToPersonageCommand;
    private final WorldRaidContributionService worldRaidContributionService;
    private final PersonageStatsService personageStatsService;

    public PersonalQuestService(
        PersonalQuestDao personalQuestDao,
        PersonageService personageService,
        LockService lockService,
        LaunchedEventService launchedEventService,
        PersonageEventService personageEventService,
        PersonalQuestConfig config,
        SendNotificationToPersonageCommand sendNotificationToPersonageCommand,
        WorldRaidContributionService worldRaidContributionService,
        PersonageStatsService personageStatsService
    ) {
        this.personalQuestDao = personalQuestDao;
        this.personageService = personageService;
        this.lockService = lockService;
        this.launchedEventService = launchedEventService;
        this.config = config;
        this.personageEventService = personageEventService;
        this.sendNotificationToPersonageCommand = sendNotificationToPersonageCommand;
        this.worldRaidContributionService = worldRaidContributionService;
        this.personageStatsService = personageStatsService;
    }

    public void save(int eventId, SavingPersonalQuest quest) {
        personalQuestDao.save(eventId, quest);
    }

    public PersonalQuestRequirements getRequirements() {
        return new PersonalQuestRequirements(
            config.requiredEnergy(),
            config.requiredTime()
        );
    }

    @Transactional
    public Either<TakeQuestError, StartedQuest> takeQuest(PersonageId personageId, int count) {
        return lockService.tryLockAndCalc(
            LockPrefixes.PERSONAGE.name() + "-" + personageId.value(),
            () -> takeQuestLogic(personageId, config.requiredEnergy(), count)
        )
            .fold(
                _ -> {
                    logger.warn("Failed to take quest for personage {}, locked", personageId);
                    return Either.left(TakeQuestError.PersonageLocked.INSTANCE);
                },
                success -> success
            );
    }

    @Transactional
    public Either<TakeQuestError, StartedQuest.Single> autoStartQuest(PersonageId personageId) {
        return lockService.tryLockAndCalc(
            LockPrefixes.PERSONAGE.name() + "-" + personageId.value(),
            () -> takeQuestLogic(personageId, config.requiredEnergyForAutoStart(), 1)
        ).fold(
            _ -> {
                logger.warn("Failed to take quest for personage {}, locked", personageId);
                return Either.left(TakeQuestError.PersonageLocked.INSTANCE);
            },
            success -> success.map(it -> (StartedQuest.Single) it)
        );
    }

    private Either<TakeQuestError, StartedQuest> takeQuestLogic(
        PersonageId personageId,
        int requiredEnergyForOne,
        int count
    ) {
        if (count < 1) {
            return Either.left(TakeQuestError.NotPositiveCount.INSTANCE);
        }
        int requiredEnergy = MathUtils.multiply(requiredEnergyForOne, count).orElse(Integer.MAX_VALUE);
        final var checkEnergyResult = personageService.checkPersonageEnergy(personageId, requiredEnergy);
        if (checkEnergyResult.isLeft()) {
            return Either.left(new TakeQuestError.NotEnoughEnergy(requiredEnergy));
        }
        final var personage = checkEnergyResult.get();

        final var presentEvents = launchedEventService.getActiveEventsByPersonageId(personageId);
        if (presentEvents.hasBlockingEvent()) {
            return Either.left(TakeQuestError.PersonageInOtherEvent.INSTANCE);
        }

        final var quest = personalQuestDao.getRandomQuest();
        if (quest.isEmpty()) {
            logger.warn("No quests in database");
            return Either.left(TakeQuestError.NoQuests.INSTANCE);
        }

        final var now = TimeUtils.moscowTime();
        final var duration = config.requiredTime().multipliedBy(count);
        final var launchedEvent = launchedEventService.createFromPersonalQuest(quest.get(), now, now.plus(duration));
        final var addResult = personageEventService.addPersonageToLaunchedEvent(
            new AddPersonageToEventRequest(
                launchedEvent.id(),
                personageId,
                Optional.of(new PersonalQuestPersonageParams(count)),
                requiredEnergy
            )
        );
        if (addResult.isLeft()) {
            logger.error("Failed to add personage {} to launched quest {}", personageId, launchedEvent.id());
            throw new IllegalStateException("Failed to add personage to launched quest");
        }

        final var reduceEnergyResult = personageService.reduceEnergy(personage, requiredEnergy, now);
        if (reduceEnergyResult.isLeft()) {
            logger.error("Personage {} has not enough energy for quest after checking", personageId);
            throw new IllegalStateException("Personage has not enough energy for quest after checking");
        }

        logger.info("Started quest {}", quest.get().code());
        if (count == 1) {
            return Either.right(new StartedQuest.Single(quest.get(), duration, requiredEnergy));
        } else {
            return Either.right(new StartedQuest.Multiple(count, duration, requiredEnergy));
        }
    }

    public EventResult.PersonalQuestEventResult stopQuest(LaunchedEvent launchedEvent) {
        final var quest = personalQuestDao.getByEventId(launchedEvent.eventId())
            .orElseThrow(() -> new IllegalStateException("Event " + launchedEvent.eventId() + " is not quest"));
        final var participants = personageEventService.getQuestParticipants(launchedEvent.id());
        if (participants.size() != 1) {
            logger.error("Quest {} has {} participants, expected 1", launchedEvent.eventId(), participants.size());
            launchedEventService.setError(launchedEvent);
            throw new IllegalStateException("Quest " + launchedEvent.eventId() + " has more than 1 participant");
        }
        final var participant = participants.getFirst();
        final var personage = participant.personage();
        final var count = participant.params().count();
        if (count < 1) {
            logger.error("Quest {} has not positive count {}", launchedEvent.eventId(), count);
            launchedEventService.setError(launchedEvent);
            throw new IllegalStateException("Quest " + launchedEvent.eventId() + " has not positive count");
        }

        final var results = processQuests(personage.id(), count);
        if (results.size() == 1) {
            final var result = results.getFirst();
            if (result instanceof PersonalQuestResult.Success(Money reward)) {
                personageService.addMoney(personage, reward);
                personageStatsService.addSuccessQuest(personage.id());
            } else {
                personageStatsService.addFailedQuest(personage.id());
            }
            launchedEventService.updateResult(launchedEvent, result);
            final var eventResult = new EventResult.PersonalQuestEventResult.Single(quest, personage, result);
            worldRaidContributionService.questComplete(personage.id());
            sendNotificationToPersonageCommand.sendNotification(
                personage.id(),
                new Notification.QuestResult(eventResult)
            );
            return eventResult;
        } else {
            final var firstResult = results.getFirst();
            launchedEventService.updateResult(launchedEvent, firstResult);
            int reward = 0;
            int successCount = 0;
            if (firstResult instanceof PersonalQuestResult.Success(Money money)) {
                reward += money.value();
                successCount++;
            }
            for (int i = 1; i < results.size(); i++) {
                if (results.get(i) instanceof PersonalQuestResult.Success(Money money)) {
                    reward += money.value();
                    successCount++;
                }
                final var launchedEventId = launchedEventService.createFinished(
                    quest,
                    TimeUtils.moscowTime(),
                    results.get(i)
                );
                personageEventService.addPersonageToLaunchedEvent(
                    new AddPersonageToEventRequest(
                        launchedEventId,
                        personage.id(),
                        Optional.of(new PersonalQuestPersonageParams(0)),
                        0
                    )
                );
            }
            personageService.addMoney(personage, Money.from(reward));
            final var eventResult = new EventResult.PersonalQuestEventResult.Multiple(personage, results);
            worldRaidContributionService.questComplete(personage.id(), count);
            personageStatsService.addQuests(personage.id(), successCount, count);
            sendNotificationToPersonageCommand.sendNotification(
                personage.id(),
                new Notification.QuestResult(eventResult)
            );
            return eventResult;
        }
    }

    private List<PersonalQuestResult> processQuests(
        PersonageId personageId,
        int count
    ) {
        int failedRow = launchedEventService.countFailedPersonalQuestsRowForPersonage(personageId);
        // За каждый неуспешный квест подряд, добавляется 10% шанс удачной попытки
        // При базовой вероятности 80%, не может быть более двух неуспешных подряд
        // При базовой вероятности 80% итоговая вероятность равна примерно 82%
        final var results = new ArrayList<PersonalQuestResult>();
        for (int i = 0; i < count; i++) {
            final var isSuccess = RandomUtils.processChance(config.baseSuccessProbability() + failedRow * 10);
            if (isSuccess) {
                failedRow = 0;
                final var reward = Money.from(RandomUtils.getInInterval(config.reward()));
                results.add(new PersonalQuestResult.Success(reward));
            } else {
                ++failedRow;
                results.add(PersonalQuestResult.Failure.INSTANCE);
            }
        }
        return results;
    }
}
