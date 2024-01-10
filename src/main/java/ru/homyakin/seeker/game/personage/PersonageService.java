package ru.homyakin.seeker.game.personage;

import io.vavr.control.Either;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.event.models.LaunchedEvent;
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

@Service
public class PersonageService {
    private static final Logger logger = LoggerFactory.getLogger(PersonageService.class);
    private final PersonageDao personageDao;
    private final LaunchedEventService launchedEventService;
    private final PersonageRaidResultDao personageRaidResultDao;
    private final EventService eventService;

    public PersonageService(
        PersonageDao personageDao,
        LaunchedEventService launchedEventService,
        PersonageRaidResultDao personageRaidResultDao,
        EventService eventService
    ) {
        this.personageDao = personageDao;
        this.launchedEventService = launchedEventService;
        this.personageRaidResultDao = personageRaidResultDao;
        this.eventService = eventService;
    }

    public Personage createPersonage() {
        final var id = personageDao.save(Personage.createDefault());
        return personageDao.getById(id)
            .orElseThrow(() -> new IllegalStateException("Personage must be present after create"));
    }

    public Either<NameError, Personage> createPersonage(String name) {
        return Personage.validateName(name)
            .map(Personage::createDefault)
            .map(personageDao::save)
            .map(id -> personageDao.getById(id).orElseThrow(() -> new IllegalStateException("Personage must be present after create")))
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
                .hasEnoughEnergyForEvent()
                .map(success -> requestedEvent)
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
                    .map(success -> requestedEvent)
                    .mapLeft(locked -> PersonageEventError.EventInProcess.INSTANCE)
                )
            );
    }

    public Optional<Personage> getById(PersonageId personageId) {
        return personageDao.getById(personageId)
            .map(personage ->
                personage.regenEnergyIfNeed()
                    .peek(personageDao::update)
                    .getOrElse(personage)
            );
    }

    public Personage getByIdForce(PersonageId personageId) {
        return getById(personageId)
            .orElseThrow(() -> new IllegalStateException("Personage must be present with id " + personageId));
    }

    public List<Personage> getByLaunchedEvent(long launchedEventId) {
        return personageDao
            .getByLaunchedEvent(launchedEventId)
            .stream()
            .map(
                personage -> personage.regenEnergyIfNeed()
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

    public Personage addMoneyAndNullifyEnergy(Personage personage, Money money, LocalDateTime energyChangeTime) {
        final var updatedPersonage = personage
            .addMoney(money)
            .nullifyEnergy(energyChangeTime);
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
        return personage.changeName(name, personageDao);
    }

    public Either<NotEnoughMoney, Personage> resetStats(Personage personage) {
        return personage
            .resetStats()
            .peek(personageDao::update);
    }
}
