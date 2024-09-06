package ru.homyakin.seeker.game.personage;

import io.vavr.control.Either;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.homyakin.seeker.game.personage.models.JoinToRaidResult;
import ru.homyakin.seeker.game.event.models.LaunchedEvent;
import ru.homyakin.seeker.game.event.raid.RaidService;
import ru.homyakin.seeker.game.event.service.LaunchedEventService;
import ru.homyakin.seeker.game.personage.badge.BadgeService;
import ru.homyakin.seeker.game.personage.models.PersonageRaidResult;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.models.PersonageRaidSavedResult;
import ru.homyakin.seeker.game.personage.models.errors.AddPersonageToRaidError;
import ru.homyakin.seeker.game.personage.models.errors.NameError;
import ru.homyakin.seeker.game.personage.models.errors.NotEnoughEnergy;
import ru.homyakin.seeker.game.personage.models.errors.NotEnoughLevelingPoints;
import ru.homyakin.seeker.game.personage.models.errors.NotEnoughMoney;
import ru.homyakin.seeker.game.tavern_menu.models.MenuItemEffect;
import ru.homyakin.seeker.utils.TimeUtils;

@Service
public class PersonageService {
    private static final Logger logger = LoggerFactory.getLogger(PersonageService.class);
    private final PersonageDao personageDao;
    private final LaunchedEventService launchedEventService;
    private final PersonageRaidResultDao personageRaidResultDao;
    private final RaidService raidService;
    private final BadgeService badgeService;
    private final PersonageConfig personageConfig;

    public PersonageService(
        PersonageDao personageDao,
        LaunchedEventService launchedEventService,
        PersonageRaidResultDao personageRaidResultDao,
        RaidService raidService,
        BadgeService badgeService,
        PersonageConfig personageConfig
    ) {
        this.personageDao = personageDao;
        this.launchedEventService = launchedEventService;
        this.personageRaidResultDao = personageRaidResultDao;
        this.raidService = raidService;
        this.badgeService = badgeService;
        this.personageConfig = personageConfig;
    }

    public Personage createPersonage() {
        final var id = personageDao.createDefault();
        badgeService.createDefaultPersonageBadge(id);
        return personageDao.getById(id)
            .orElseThrow(() -> new IllegalStateException("Personage must be present after create"));
    }

    public Either<NameError, Personage> createPersonage(String name) {
        return Personage.validateName(name)
            .map(personageDao::createDefault)
            .peek(badgeService::createDefaultPersonageBadge)
            .map(id -> personageDao.getById(id).orElseThrow(() -> new IllegalStateException("Personage must be present after create")))
            .peekLeft(_ -> logger.warn("Can't create personage with name " + name));
    }

    @Transactional
    public Either<AddPersonageToRaidError, JoinToRaidResult> joinRaid(PersonageId personageId, long launchedEventId) {
        final var launchedEvent = launchedEventService.getById(launchedEventId);
        if (launchedEvent.isEmpty()) {
            logger.warn("Personage {} tried to join to not created event {}", personageId, launchedEventId);
            return Either.left(AddPersonageToRaidError.RaidNotExist.INSTANCE);
        }
        final var raid = raidService.getByEventId(launchedEvent.get().eventId());
        if (raid.isEmpty()) {
            logger.warn("Personage {} tried to join to not raid event {}", personageId, launchedEventId);
            return Either.left(AddPersonageToRaidError.RaidNotExist.INSTANCE);
        }
        if (launchedEvent.get().isInFinalStatus()) {
            logger.warn("Personage {} tried to join to ended event {}", personageId, launchedEventId);
            return Either.left(new AddPersonageToRaidError.EndedRaid(launchedEvent.get(), raid.get()));
        }

        final var presentEvent = launchedEventService.getActiveEventByPersonageId(personageId);
        if (presentEvent.isPresent()) {
            if (Objects.equals(launchedEvent.get().id(), presentEvent.get().id())) {
                return Either.left(AddPersonageToRaidError.PersonageInThisRaid.INSTANCE);
            }
            return Either.left(AddPersonageToRaidError.PersonageInOtherEvent.INSTANCE);
        }

        final var checkEnergyResult = checkPersonageEnergy(
            personageId,
            personageConfig.raidEnergyCost()
        );

        if (checkEnergyResult.isLeft()) {
            return Either.left(new AddPersonageToRaidError.NotEnoughEnergy(personageConfig.raidEnergyCost()));
        }

        return launchedEventService.addPersonageToLaunchedEvent(personageId, launchedEventId)
            .<AddPersonageToRaidError>mapLeft(_ -> AddPersonageToRaidError.RaidInProcess.INSTANCE)
            .map(_ -> {
                final var reduceResult = reduceEnergy(
                    checkEnergyResult.get(),
                    personageConfig.raidEnergyCost(),
                    TimeUtils.moscowTime()
                );
                if (reduceResult.isLeft()) {
                    logger.error("Personage {} has not enough energy for raid after checking", personageId);
                    throw new IllegalStateException("Personage has not enough energy for raid after checking");
                }
                return new JoinToRaidResult(launchedEvent.get(), raid.get(), getByLaunchedEvent(launchedEventId));
            });
    }

    public Optional<Personage> getById(PersonageId personageId) {
        return personageDao.getById(personageId)
            .map(personage ->
                personage.updateStateIfNeed(TimeUtils.moscowTime())
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
                personage -> personage.updateStateIfNeed(now)
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

    public Personage addMoney(Personage personage, Money money, LocalDateTime energyChangeTime) {
        final var updatedPersonage = personage
            .addMoney(money);
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

    public Either<NotEnoughEnergy, Personage> checkPersonageEnergy(PersonageId personageId, int requiredEnergy) {
        final var personage = getByIdForce(personageId);
        if (!personage.hasEnoughEnergy(requiredEnergy)) {
            return Either.left(NotEnoughEnergy.INSTANCE);
        }
        return Either.right(personage);
    }

    public Either<NotEnoughEnergy, Personage> reduceEnergy(Personage personage, int requiredEnergy, LocalDateTime time) {
        return personage.reduceEnergy(time, requiredEnergy).peek(personageDao::update);
    }

    public boolean toggleIsHidden(PersonageId personageId) {
        return personageDao.toggleIsHidden(personageId);
    }
}
