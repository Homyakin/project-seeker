package ru.homyakin.seeker.game.event.personal_quest;

import io.vavr.control.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.homyakin.seeker.game.event.models.EventResult;
import ru.homyakin.seeker.game.event.launched.LaunchedEvent;
import ru.homyakin.seeker.game.event.personal_quest.model.PersonalQuestRequirements;
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
import ru.homyakin.seeker.infrastructure.init.saving_models.SavingPersonalQuest;
import ru.homyakin.seeker.infrastructure.lock.LockPrefixes;
import ru.homyakin.seeker.infrastructure.lock.LockService;
import ru.homyakin.seeker.utils.RandomUtils;
import ru.homyakin.seeker.utils.TimeUtils;

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

    public PersonalQuestService(
        PersonalQuestDao personalQuestDao,
        PersonageService personageService,
        LockService lockService,
        LaunchedEventService launchedEventService,
        PersonageEventService personageEventService,
        PersonalQuestConfig config,
        SendNotificationToPersonageCommand sendNotificationToPersonageCommand,
        WorldRaidContributionService worldRaidContributionService
    ) {
        this.personalQuestDao = personalQuestDao;
        this.personageService = personageService;
        this.lockService = lockService;
        this.launchedEventService = launchedEventService;
        this.config = config;
        this.personageEventService = personageEventService;
        this.sendNotificationToPersonageCommand = sendNotificationToPersonageCommand;
        this.worldRaidContributionService = worldRaidContributionService;
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
    public Either<TakeQuestError, StartedQuest> takeQuest(PersonageId personageId) {
        return lockService.tryLockAndCalc(
            LockPrefixes.PERSONAGE.name() + "-" + personageId.value(),
            () -> takeQuestLogic(personageId, config.requiredEnergy())
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
    public Either<TakeQuestError, StartedQuest> autoStartQuest(PersonageId personageId) {
        return lockService.tryLockAndCalc(
            LockPrefixes.PERSONAGE.name() + "-" + personageId.value(),
            () -> takeQuestLogic(personageId, config.requiredEnergyForAutoStart())
        ).fold(
            _ -> {
                logger.warn("Failed to take quest for personage {}, locked", personageId);
                return Either.left(TakeQuestError.PersonageLocked.INSTANCE);
            },
            success -> success
        );
    }

    private Either<TakeQuestError, StartedQuest> takeQuestLogic(PersonageId personageId, int requiredEnergy) {
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
        final var launchedEvent = launchedEventService.createFromPersonalQuest(quest.get(), now, now.plus(config.requiredTime()));
        final var addResult = personageEventService.addPersonageToLaunchedEvent(
            new AddPersonageToEventRequest(
                launchedEvent.id(),
                personageId,
                Optional.empty()
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
        return Either.right(new StartedQuest(quest.get(), config.requiredTime(), requiredEnergy));
    }

    public EventResult.PersonalQuestResult stopQuest(LaunchedEvent launchedEvent) {
        final var quest = personalQuestDao.getByEventId(launchedEvent.eventId())
            .orElseThrow(() -> new IllegalStateException("Event " + launchedEvent.eventId() + " is not quest"));
        final var participants = personageEventService.getQuestParticipants(launchedEvent.id());
        if (participants.size() != 1) {
            logger.error("Quest {} has {} participants, expected 1", launchedEvent.eventId(), participants.size());
            final var result = EventResult.PersonalQuestResult.Error.INSTANCE;
            launchedEventService.updateResult(launchedEvent, result);
            return result;
        }
        final var personage = participants.getFirst().personage();

        final var failedRow = launchedEventService.countFailedPersonalQuestsRowForPersonage(personage.id());
        // За каждый неуспешный квест подряд, добавляется 10% шанс удачной попытки
        // При базовой вероятности 80%, не может быть более двух неуспешных подряд
        // При базовой вероятности 80% итоговая вероятность равна примерно 82%
        final var isSuccess = RandomUtils.processChance(config.baseSuccessProbability() + failedRow * 10);
        final EventResult.PersonalQuestResult result;
        if (isSuccess) {
            logger.info("Quest {} succeeded by personage {}", launchedEvent.id(), personage.id());
            final var reward = Money.from(RandomUtils.getInInterval(config.reward()));
            personageService.addMoney(personage, reward);
            final var success = new EventResult.PersonalQuestResult.Success(quest, personage, reward);
            sendNotificationToPersonageCommand
                .sendNotification(personage.id(), new Notification.SuccessQuestResult(success));
            result = success;
        } else {
            logger.info("Quest {} failed by personage {}", launchedEvent.id(), personage.id());
            final var failure = new EventResult.PersonalQuestResult.Failure(quest, personage);
            sendNotificationToPersonageCommand
                .sendNotification(personage.id(), new Notification.FailureQuestResult(failure));
            result = failure;
        }

        launchedEventService.updateResult(launchedEvent, result);
        worldRaidContributionService.questComplete(personage.id());
        return result;
    }
}
