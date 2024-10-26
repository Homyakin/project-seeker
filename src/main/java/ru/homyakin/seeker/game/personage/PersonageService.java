package ru.homyakin.seeker.game.personage;

import io.vavr.control.Either;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.event.launched.LaunchedEvent;
import ru.homyakin.seeker.game.personage.badge.BadgeService;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffect;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffectType;
import ru.homyakin.seeker.game.personage.models.PersonageRaidResult;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.models.PersonageRaidSavedResult;
import ru.homyakin.seeker.game.utils.NameError;
import ru.homyakin.seeker.game.personage.models.errors.NotEnoughEnergy;
import ru.homyakin.seeker.game.personage.models.errors.NotEnoughLevelingPoints;
import ru.homyakin.seeker.game.personage.models.errors.NotEnoughMoney;
import ru.homyakin.seeker.game.utils.NameValidator;
import ru.homyakin.seeker.utils.TimeUtils;

@Service
public class PersonageService {
    private static final Logger logger = LoggerFactory.getLogger(PersonageService.class);
    private final PersonageDao personageDao;
    private final PersonageRaidResultDao personageRaidResultDao;
    private final BadgeService badgeService;

    public PersonageService(
        PersonageDao personageDao,
        PersonageRaidResultDao personageRaidResultDao,
        BadgeService badgeService
    ) {
        this.personageDao = personageDao;
        this.personageRaidResultDao = personageRaidResultDao;
        this.badgeService = badgeService;
    }

    public Personage createPersonage() {
        final var id = personageDao.createDefault();
        badgeService.createDefaultPersonageBadge(id);
        return personageDao.getById(id)
            .orElseThrow(() -> new IllegalStateException("Personage must be present after create"));
    }

    public Either<NameError, Personage> createPersonage(String name) {
        return NameValidator.validateName(name)
            .map(personageDao::createDefault)
            .peek(badgeService::createDefaultPersonageBadge)
            .map(id -> personageDao.getById(id).orElseThrow(() -> new IllegalStateException("Personage must be present after create")))
            .peekLeft(_ -> logger.warn("Can't create personage with name " + name));
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

    public List<Personage> getByIds(Set<PersonageId> ids) {
        final var now = TimeUtils.moscowTime();
        return personageDao.getByIds(ids)
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

    public Optional<PersonageRaidSavedResult> getRaidResult(PersonageId personageId, long launchedEventId) {
        return personageRaidResultDao.getByPersonageAndEvent(personageId, launchedEventId);
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

    public Personage addEffect(Personage personage, PersonageEffectType type, PersonageEffect effect) {
        final var updatedPersonage = personage.addEffect(type, effect);
        personageDao.update(updatedPersonage);
        return updatedPersonage;
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

    public long getActivePersonagesCount(LocalDateTime start) {
        return personageDao.getActivePersonagesCount(start);
    }
}
