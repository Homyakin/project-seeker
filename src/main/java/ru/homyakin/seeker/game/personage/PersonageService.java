package ru.homyakin.seeker.game.personage;

import io.vavr.control.Either;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.badge.action.PersonageBadgeService;
import ru.homyakin.seeker.game.event.launched.LaunchedEvent;
import ru.homyakin.seeker.game.event.world_raid.entity.battle.PersonageWorldRaidBattleResult;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.personage.models.BattleType;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffect;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffectType;
import ru.homyakin.seeker.game.personage.models.PersonageRaidResult;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.models.PersonageBattleResult;
import ru.homyakin.seeker.game.utils.NameError;
import ru.homyakin.seeker.game.personage.models.errors.NotEnoughEnergy;
import ru.homyakin.seeker.game.personage.models.errors.NotEnoughLevelingPoints;
import ru.homyakin.seeker.game.personage.models.errors.NotEnoughMoney;
import ru.homyakin.seeker.game.utils.NameValidator;
import ru.homyakin.seeker.utils.TimeUtils;
import ru.homyakin.seeker.utils.models.Success;

@Service
public class PersonageService {
    private static final Logger logger = LoggerFactory.getLogger(PersonageService.class);
    private final PersonageDao personageDao;
    private final PersonageBattleResultDao personageBattleResultDao;
    private final PersonageBadgeService personageBadgeService;

    public PersonageService(
        PersonageDao personageDao,
        PersonageBattleResultDao personageBattleResultDao,
        PersonageBadgeService personageBadgeService
    ) {
        this.personageDao = personageDao;
        this.personageBattleResultDao = personageBattleResultDao;
        this.personageBadgeService = personageBadgeService;
    }

    public Personage createPersonage() {
        final var id = personageDao.createDefault();
        personageBadgeService.createDefaultPersonageBadge(id);
        return personageDao.getById(id)
            .orElseThrow(() -> new IllegalStateException("Personage must be present after create"));
    }

    public Either<NameError, Personage> createPersonage(String name) {
        return NameValidator.validateName(name)
            .map(personageDao::createDefault)
            .peek(personageBadgeService::createDefaultPersonageBadge)
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

    public List<Personage> getByGroupId(GroupId groupId) {
        final var ids = personageDao.getPersonageIdsByGroupId(groupId);
        return personageDao.getByIds(new java.util.HashSet<>(ids));
        }

    public Personage getByIdForce(PersonageId personageId) {
        return getById(personageId)
            .orElseThrow(() -> new IllegalStateException("Personage must be present with id " + personageId));
    }

    public List<Personage> getByIdsWithoutEnergyRegen(Set<PersonageId> ids) {
        final var now = TimeUtils.moscowTime();
        return personageDao.getByIds(ids)
            .stream()
            .map(
                personage -> personage.updateStateIfNeed(now, false)
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

    public void addMoney(PersonageId personageId, Money money) {
        addMoney(getByIdForce(personageId), money);
    }

    public void addMoneyBatch(Map<PersonageId, Money> moneyMap) {
        personageDao.addMoney(moneyMap);
    }

    public void saveRaidResults(List<PersonageRaidResult> results, LaunchedEvent launchedEvent) {
        personageBattleResultDao.saveBatch(
            results.stream()
                .map(result -> new PersonageBattleResult(
                    result.participant().personage().id(),
                    launchedEvent.id(),
                    result.stats(),
                    result.reward(),
                    result.generatedItem().map(Item::id)
                ))
                .toList()
        );
    }

    public void saveWorldRaidResults(List<PersonageWorldRaidBattleResult> results, LaunchedEvent launchedEvent) {
        personageDao.addMoney(
            results.stream()
                .collect(
                    Collectors.toMap(
                        PersonageWorldRaidBattleResult::personageId,
                        PersonageWorldRaidBattleResult::reward
                    )
                )
        );
        personageBattleResultDao.saveBatch(
            results.stream()
                .map(result -> new PersonageBattleResult(
                    result.personage().id(),
                    launchedEvent.id(),
                    result.stats(),
                    result.reward(),
                    result.generatedItem().map(Item::id)
                ))
                .toList()
        );
    }

    public Optional<PersonageBattleResult> getLastRaidResult(PersonageId personageId) {
        return personageBattleResultDao.getLastByPersonage(personageId, BattleType.RAID);
    }

    public Optional<PersonageBattleResult> getBattleResult(PersonageId personageId, long launchedEventId) {
        return personageBattleResultDao.getByPersonageAndEvent(personageId, launchedEventId);
    }

    public Either<NotEnoughMoney, Success> tryTakeMoney(PersonageId personageId, Money money) {
        final var personage = getByIdForce(personageId);
        if (personage.money().lessThan(money)) {
            return Either.left(new NotEnoughMoney(money));
        }
        addMoney(personage, money.negative());
        return Either.right(Success.INSTANCE);
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
        return personageBattleResultDao.countSuccessRaidsFromLastItem(personageId);
    }

    public int countWorldRaidsFromLastItem(PersonageId personageId) {
        return personageBattleResultDao.countWorldRaidsFromLastItem(personageId);
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

    public Personage addEffect(PersonageId personageId, PersonageEffectType type, PersonageEffect effect) {
        return addEffect(getByIdForce(personageId), type, effect);
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

    /**
     * @return Возвращает добавленную энергию
     */
    public int addEnergy(PersonageId personageId, int energy) {
        if (energy == 0) {
            return 0;
        }
        final var personage = getByIdForce(personageId);
        final var updated = personage.addEnergy(TimeUtils.moscowTime(), energy);
        personageDao.update(updated);
        return updated.energy().value() - personage.energy().value();
    }
}
