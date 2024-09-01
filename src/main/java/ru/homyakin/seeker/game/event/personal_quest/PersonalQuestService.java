package ru.homyakin.seeker.game.event.personal_quest;

import io.vavr.control.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.homyakin.seeker.game.event.models.EventResult;
import ru.homyakin.seeker.game.event.models.LaunchedEvent;
import ru.homyakin.seeker.game.event.personal_quest.model.PersonalQuestRequirements;
import ru.homyakin.seeker.game.event.personal_quest.model.StartedQuest;
import ru.homyakin.seeker.game.event.personal_quest.model.TakeQuestError;
import ru.homyakin.seeker.game.event.service.LaunchedEventService;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.infrastructure.init.saving_models.SavingPersonalQuest;
import ru.homyakin.seeker.infrastructure.lock.LockPrefixes;
import ru.homyakin.seeker.infrastructure.lock.LockService;
import ru.homyakin.seeker.utils.RandomUtils;
import ru.homyakin.seeker.utils.TimeUtils;

@Service
public class PersonalQuestService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final PersonalQuestDao personalQuestDao;
    private final PersonageService personageService;
    private final LockService lockService;
    private final LaunchedEventService launchedEventService;
    private final PersonalQuestConfig config;

    public PersonalQuestService(
        PersonalQuestDao personalQuestDao,
        PersonageService personageService,
        LockService lockService,
        LaunchedEventService launchedEventService,
        PersonalQuestConfig config
    ) {
        this.personalQuestDao = personalQuestDao;
        this.personageService = personageService;
        this.lockService = lockService;
        this.launchedEventService = launchedEventService;
        this.config = config;
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
            () -> takeQuestLogic(personageId)
        )
            .fold(
                _ -> {
                    logger.warn("Failed to take quest for personage {}, locked", personageId);
                    return Either.left(TakeQuestError.PersonageLocked.INSTANCE);
                },
                success -> success
            );
    }

    private Either<TakeQuestError, StartedQuest> takeQuestLogic(PersonageId personageId) {
        final var checkEnergyResult = personageService.checkPersonageEnergy(personageId, config.requiredEnergy());
        if (checkEnergyResult.isLeft()) {
            return Either.left(new TakeQuestError.NotEnoughEnergy(config.requiredEnergy()));
        }
        final var personage = checkEnergyResult.get();

        final var presentEvent = launchedEventService.getActiveEventByPersonageId(personageId);
        if (presentEvent.isPresent()) {
            return Either.left(TakeQuestError.PersonageInOtherEvent.INSTANCE);
        }

        final var quest = personalQuestDao.getRandomQuest();
        if (quest.isEmpty()) {
            logger.warn("No quests in database");
            return Either.left(TakeQuestError.NoQuests.INSTANCE);
        }

        final var now = TimeUtils.moscowTime();
        final var launchedEvent = launchedEventService.createFromPersonalQuest(quest.get(), now, now.plus(config.requiredTime()));
        final var addResult = launchedEventService.addPersonageToLaunchedEvent(personageId, launchedEvent.id());
        if (addResult.isLeft()) {
            logger.error("Failed to add personage {} to launched quest {}", personageId, launchedEvent.id());
            throw new IllegalStateException("Failed to add personage to launched quest");
        }

        final var reduceEnergyResult = personageService.reduceEnergy(personage, config.requiredEnergy(), now);
        if (reduceEnergyResult.isLeft()) {
            logger.error("Personage {} has not enough energy for quest after checking", personageId);
            throw new IllegalStateException("Personage has not enough energy for quest after checking");
        }

        logger.info("Started quest {}", quest.get().code());
        return Either.right(new StartedQuest(quest.get(), config.requiredTime()));
    }

    public EventResult.PersonalQuestResult stopQuest(LaunchedEvent launchedEvent) {
        final var quest = personalQuestDao.getByEventId(launchedEvent.eventId())
            .orElseThrow(() -> new IllegalStateException("Event " + launchedEvent.eventId() + " is not quest"));
        final var personages = personageService.getByLaunchedEvent(launchedEvent.id());
        if (personages.size() != 1) {
            logger.error("Quest {} has {} personages, expected 1", launchedEvent.eventId(), personages.size());
            final var result = EventResult.PersonalQuestResult.Error.INSTANCE;
            launchedEventService.updateResult(launchedEvent, result);
            return result;
        }
        final var personage = personages.getFirst();

        final var isSuccess = RandomUtils.processChance(config.successProbability());
        final EventResult.PersonalQuestResult result;
        if (isSuccess) {
            logger.info("Quest {} succeeded", launchedEvent.id());
            final var reward = Money.from(RandomUtils.getInInterval(config.reward()));
            personageService.addMoney(personage, reward);
            result = new EventResult.PersonalQuestResult.Success(quest, personage, reward);
        } else {
            logger.info("Quest {} failed", launchedEvent.id());
            result = new EventResult.PersonalQuestResult.Failure(quest, personage);
        }

        launchedEventService.updateResult(launchedEvent, result);
        return result;
    }
}
