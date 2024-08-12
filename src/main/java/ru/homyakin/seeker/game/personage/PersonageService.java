package ru.homyakin.seeker.game.personage;

import io.vavr.control.Either;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.event.models.LaunchedEvent;
import ru.homyakin.seeker.game.personage.badge.BadgeService;
import ru.homyakin.seeker.game.personage.models.PersonageRaidResult;
import ru.homyakin.seeker.game.event.service.EventService;
import ru.homyakin.seeker.game.event.service.LaunchedEventService;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.models.PersonageRaidSavedResult;
import ru.homyakin.seeker.game.personage.models.errors.NameError;
import ru.homyakin.seeker.game.personage.models.errors.NotEnoughLevelingPoints;
import ru.homyakin.seeker.game.personage.models.errors.NotEnoughMoney;
import ru.homyakin.seeker.game.personage.models.errors.PersonageEventError;
import ru.homyakin.seeker.game.tavern_menu.models.MenuItemEffect;
import ru.homyakin.seeker.utils.TimeUtils;

@Service
public class PersonageService {
    private static final Logger logger = LoggerFactory.getLogger(PersonageService.class);
    private final PersonageDao personageDao;
    private final LaunchedEventService launchedEventService;
    private final PersonageRaidResultDao personageRaidResultDao;
    private final EventService eventService;
    private final BadgeService badgeService;
    private final PersonageConfig personageConfig;

    public PersonageService(
        PersonageDao personageDao,
        LaunchedEventService launchedEventService,
        PersonageRaidResultDao personageRaidResultDao,
        EventService eventService,
        BadgeService badgeService,
        PersonageConfig personageConfig
    ) {
        this.personageDao = personageDao;
        this.launchedEventService = launchedEventService;
        this.personageRaidResultDao = personageRaidResultDao;
        this.eventService = eventService;
        this.badgeService = badgeService;
        this.personageConfig = personageConfig;
    }

    public Personage createPersonage() {
        final var id = personageDao.save(Personage.createDefault());
        badgeService.createDefaultPersonageBadge(id);
        return personageDao.getById(id)
            .orElseThrow(() -> new IllegalStateException("Personage must be present after create"));
    }

    public Either<NameError, Personage> createPersonage(String name) {
        return Personage.validateName(name)
            .map(Personage::createDefault)
            .map(personageDao::save)
            .map(id -> {
                badgeService.createDefaultPersonageBadge(id);
                return personageDao.getById(id).orElseThrow(() -> new IllegalStateException("Personage must be present after create"));
            })
            .peekLeft(error -> logger.warn("Can't create personage with name " + name));
    }

    public Either<PersonageEventError, LaunchedEvent> addEvent(PersonageId personageId, long launchedEventId) {
        return launchedEventService.getById(launchedEventId)
            .<Either<PersonageEventError, LaunchedEvent>>map(Either::right)
            .orElse(Either.left(PersonageEventError.EventNotExist.INSTANCE))
            .filterOrElse(
                LaunchedEvent::isNotInFinalStatus,
                requestedEvent -> eventService.getEventById(requestedEvent.eventId())
                    .<PersonageEventError>map(PersonageEventError.ExpiredEvent::new)
                    .orElse(PersonageEventError.EventNotExist.INSTANCE)
            )
            .flatMap(requestedEvent -> getByIdForce(personageId)
                .hasEnoughEnergyForEvent(personageConfig.raidEnergyCost())
                .map(_ -> requestedEvent)
            )
            .flatMap(requestedEvent -> launchedEventService
                .getActiveEventByPersonageId(personageId)
                .<Either<PersonageEventError, LaunchedEvent>>map(activeEvent -> {
                    if (activeEvent.id() == launchedEventId) {
                        return Either.left(PersonageEventError.PersonageInThisEvent.INSTANCE);
                    } else {
                        return Either.left(PersonageEventError.PersonageInOtherEvent.INSTANCE);
                    }
                })
                .orElseGet(() -> launchedEventService
                    .addPersonageToLaunchedEvent(personageId, launchedEventId)
                    .map(_ -> requestedEvent)
                    .mapLeft(_ -> PersonageEventError.EventInProcess.INSTANCE)
                )
            );
    }

    public Optional<Personage> getById(PersonageId personageId) {
        return personageDao.getById(personageId)
            .map(personage ->
                personage.updateStateIfNeed(personageConfig.energyFullRecovery(), TimeUtils.moscowTime())
                    .peek(personageDao::update)
                    .getOrElse(personage)
            );
    }

    public Personage getByIdForce(PersonageId personageId) {
        return getById(personageId)
            .orElseThrow(() -> new IllegalStateException("Personage must be present with id " + personageId));
    }

    public List<Personage> getByLaunchedEvent(long launchedEventId) {
        final var now = TimeUtils.moscowTime();
        return personageDao
            .getByLaunchedEvent(launchedEventId)
            .stream()
            .map(
                personage -> personage.updateStateIfNeed(personageConfig.energyFullRecovery(), now)
                    .peek(personageDao::update)
                    .getOrElse(personage)
            )
            .toList();
    }

    public Personage addMoney(Personage personage, Money money) {
        final var updatedPersonage = personage.addMoney(money);
        personageDao.update(updatedPersonage);
        return updatedPersonage;
    }

    public Personage addMoneyAndReduceEnergyForEvent(Personage personage, Money money, LocalDateTime energyChangeTime) {
        final var updatedPersonage = personage
            .addMoney(money)
            .reduceEnergy(energyChangeTime, personageConfig.raidEnergyCost());
        personageDao.update(updatedPersonage);
        return updatedPersonage;
    }

    public void saveRaidResults(List<PersonageRaidResult> results, LaunchedEvent launchedEvent) {
        personageRaidResultDao.saveBatch(results, launchedEvent);
    }

    public Optional<PersonageRaidSavedResult> getLastRaidResult(PersonageId personageId) {
        return personageRaidResultDao.getLastByPersonage(personageId);
    }

    public Optional<PersonageRaidSavedResult> getRaidResult(PersonageId personageId, LaunchedEvent launchedEvent) {
        return personageRaidResultDao.getByPersonageAndEvent(personageId, launchedEvent);
    }

    public Personage takeMoney(Personage personage, Money money) {
        return addMoney(personage, money.negative());
    }

    public Either<NotEnoughLevelingPoints, Personage> incrementStrength(Personage personage) {
        return personage.incrementStrength().peek(personageDao::update);
    }

    public Either<NotEnoughLevelingPoints, Personage> incrementAgility(Personage personage) {
        return personage.incrementAgility().peek(personageDao::update);
    }

    public Either<NotEnoughLevelingPoints, Personage> incrementWisdom(Personage personage) {
        return personage.incrementWisdom().peek(personageDao::update);
    }

    public Either<NameError, Personage> changeName(Personage personage, String name) {
        return personage.changeName(name).peek(personageDao::update);
    }

    public int countSuccessRaidsFromLastItem(PersonageId personageId) {
        return personageRaidResultDao.countSuccessRaidsFromLastItem(personageId);
    }

    public Either<NotEnoughMoney, Personage> resetStats(Personage personage) {
        return personage
            .resetStats()
            .peek(personageDao::update);
    }

    /**
     * @return Either.Left если денег недостаточно, Either.Right - количество успешно списанных денег
     */
    public Either<NotEnoughMoney, Money> initChangeName(PersonageId id) {
        return getByIdForce(id).initChangeName().peek(personageDao::update).map(_ -> Personage.CHANGE_NAME_COST);
    }

    public void cancelChangeName(PersonageId id) {
        getByIdForce(id).cancelChangeName().peek(personageDao::update);
    }

    public void addMenuItemEffect(Personage personage, MenuItemEffect effect) {
        personageDao.update(personage.addMenuItemEffect(effect));
    }
}
