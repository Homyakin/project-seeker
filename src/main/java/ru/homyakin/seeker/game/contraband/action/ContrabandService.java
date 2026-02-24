package ru.homyakin.seeker.game.contraband.action;

import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.vavr.control.Either;
import ru.homyakin.seeker.game.contraband.entity.Contraband;
import ru.homyakin.seeker.game.contraband.entity.ContrabandConfig;
import ru.homyakin.seeker.game.contraband.entity.ContrabandOpenResult;
import ru.homyakin.seeker.game.contraband.entity.ContrabandPenalty;
import ru.homyakin.seeker.game.contraband.entity.ContrabandReward;
import ru.homyakin.seeker.game.contraband.entity.ContrabandStatus;
import ru.homyakin.seeker.game.contraband.entity.ContrabandStorage;
import ru.homyakin.seeker.game.contraband.entity.ContrabandTier;
import ru.homyakin.seeker.game.contraband.entity.FindReceiver;
import ru.homyakin.seeker.game.contraband.entity.FinderContrabandError;
import ru.homyakin.seeker.game.contraband.entity.ReceiverContrabandError;
import ru.homyakin.seeker.game.effect.Effect;
import ru.homyakin.seeker.game.effect.EffectCharacteristic;
import ru.homyakin.seeker.game.item.ItemService;
import ru.homyakin.seeker.game.item.models.GenerateItemParams;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffect;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffectType;
import ru.homyakin.seeker.infrastructure.lock.LockPrefixes;
import ru.homyakin.seeker.infrastructure.lock.LockService;
import ru.homyakin.seeker.utils.RandomUtils;

@Service
public class ContrabandService {
    private static final Logger logger = LoggerFactory.getLogger(ContrabandService.class);
    private final ContrabandStorage contrabandStorage;
    private final ContrabandConfig config;
    private final PersonageService personageService;
    private final ItemService itemService;
    private final FindReceiver findReceiver;
    private final LockService lockService;

    public ContrabandService(
        ContrabandStorage contrabandStorage,
        ContrabandConfig config,
        PersonageService personageService,
        ItemService itemService,
        FindReceiver findReceiver,
        LockService lockService
    ) {
        this.contrabandStorage = contrabandStorage;
        this.config = config;
        this.personageService = personageService;
        this.itemService = itemService;
        this.findReceiver = findReceiver;
        this.lockService = lockService;
    }

    public Optional<Contraband> tryCreate(Personage finder, int raidLevel) {
        final var raidsWithoutContraband = personageService.countSuccessRaidsFromLastContraband(finder.id());
        final var chance = config.dropChancePercent() + raidsWithoutContraband * 20;
        if (!RandomUtils.processChance(chance)) {
            return Optional.empty();
        }
        return Optional.of(create(finder, raidLevel));
    }

    private Contraband create(Personage finder, int raidLevel) {
        final var tier = config.tierForRaidLevel(raidLevel);
        final var now = LocalDateTime.now();
        final var contraband = new Contraband(
            0, tier, finder.id(), Optional.empty(),
            ContrabandStatus.FOUND, now, now.plusHours(config.expirationHours()), Optional.empty()
        );
        final var id = contrabandStorage.create(contraband);
        logger.info("Created contraband {} tier {} for personage {}", id, tier, finder.id());
        return contrabandStorage.getById(id).orElseThrow();
    }

    public Optional<Contraband> getById(long id) {
        return contrabandStorage.getById(id);
    }

    public Optional<Contraband> getActiveContraband(PersonageId personageId) {
        return contrabandStorage.findActiveForPersonage(personageId);
    }

    public Either<FinderContrabandError, ContrabandOpenResult> forceOpen(long contrabandId, PersonageId personageId) {
        return withLock(contrabandId, () -> forceOpenInternal(contrabandId, personageId));
    }

    public Either<FinderContrabandError, Money> sellToBlackMarket(long contrabandId, PersonageId personageId) {
        return withLock(contrabandId, () -> sellToBlackMarketInternal(contrabandId, personageId));
    }

    public Either<ReceiverContrabandError, ContrabandOpenResult> openAsReceiver(long contrabandId, PersonageId personageId) {
        return withLock(contrabandId, () -> openAsReceiverInternal(contrabandId, personageId));
    }

    private <T> T withLock(long contrabandId, java.util.function.Supplier<T> action) {
        final var key = LockPrefixes.CONTRABAND.name() + contrabandId;
        return lockService.tryLockAndCalc(key, action).getOrElseThrow(
            () -> new IllegalStateException("Failed to acquire lock for contraband " + contrabandId)
        );
    }

    @Transactional
    private Either<FinderContrabandError, ContrabandOpenResult> forceOpenInternal(long contrabandId, PersonageId personageId) {
        final var contrabandOpt = contrabandStorage.getById(contrabandId);
        if (contrabandOpt.isEmpty()) {
            return Either.left(FinderContrabandError.NotFound.INSTANCE);
        }
        final var contraband = contrabandOpt.get();
        if (!contraband.canBeAccessedBy(personageId)) {
            return Either.left(FinderContrabandError.AlreadyProcessed.INSTANCE);
        }
        if (contraband.isExpired(LocalDateTime.now())) {
            contrabandStorage.update(contraband.withStatus(ContrabandStatus.EXPIRED, LocalDateTime.now()));
            return Either.left(FinderContrabandError.Expired.INSTANCE);
        }

        final var now = LocalDateTime.now();
        if (RandomUtils.processChance(config.finderSuccessChancePercent())) {
            final var reward = generateReward(contraband.tier(), personageId);
            contrabandStorage.update(contraband.withStatus(ContrabandStatus.OPENED_SUCCESS, now));
            return Either.right(reward);
        } else {
            final var penalty = applyPenalty(contraband.tier(), personageId);
            contrabandStorage.update(contraband.withStatus(ContrabandStatus.OPENED_FAILURE, now));
            return Either.right(penalty);
        }
    }

    @Transactional
    private Either<FinderContrabandError, Money> sellToBlackMarketInternal(long contrabandId, PersonageId personageId) {
        final var contrabandOpt = contrabandStorage.getById(contrabandId);
        if (contrabandOpt.isEmpty()) {
            return Either.left(FinderContrabandError.NotFound.INSTANCE);
        }
        final var contraband = contrabandOpt.get();
        if (!contraband.finderPersonageId().equals(personageId)) {
            return Either.left(FinderContrabandError.NotOwner.INSTANCE);
        }
        if (!contraband.canBeProcessedByFinder()) {
            return Either.left(FinderContrabandError.AlreadyProcessed.INSTANCE);
        }
        if (contraband.isExpired(LocalDateTime.now())) {
            contrabandStorage.update(contraband.withStatus(ContrabandStatus.EXPIRED, LocalDateTime.now()));
            return Either.left(FinderContrabandError.Expired.INSTANCE);
        }

        final var sellPrice = config.sellPrice(contraband.tier());
        personageService.addMoney(personageId, sellPrice);
        contrabandStorage.update(contraband.withStatus(ContrabandStatus.SOLD_TO_MARKET, LocalDateTime.now()));
        logger.info("Contraband {} sold to black market by personage {}", contrabandId, personageId);
        return Either.right(sellPrice);
    }

    @Transactional
    private Either<ReceiverContrabandError, ContrabandOpenResult> openAsReceiverInternal(long contrabandId, PersonageId personageId) {
        final var contrabandOpt = contrabandStorage.getById(contrabandId);
        if (contrabandOpt.isEmpty()) {
            return Either.left(ReceiverContrabandError.NotFound.INSTANCE);
        }
        final var contraband = contrabandOpt.get();
        if (!contraband.canBeAccessedBy(personageId)) {
            return Either.left(ReceiverContrabandError.NotReceiver.INSTANCE);
        }
        if (contraband.isExpired(LocalDateTime.now())) {
            contrabandStorage.update(contraband.withStatus(ContrabandStatus.EXPIRED, LocalDateTime.now()));
            return Either.left(ReceiverContrabandError.Expired.INSTANCE);
        }

        final var now = LocalDateTime.now();
        if (RandomUtils.processChance(config.receiverSuccessChancePercent())) {
            final var reward = generateReward(contraband.tier(), personageId);
            contrabandStorage.update(contraband.withStatus(ContrabandStatus.OPENED_SUCCESS, now));
            return Either.right(reward);
        } else {
            final var penalty = applyPenalty(contraband.tier(), personageId);
            contrabandStorage.update(contraband.withStatus(ContrabandStatus.OPENED_FAILURE, now));
            return Either.right(penalty);
        }
    }

    public Optional<Contraband> assignReceiver(Contraband contraband) {
        final var receiver = findReceiver.findReceiver(contraband, config.receiverActivityDuration());
        if (receiver.isEmpty()) {
            logger.info("No receiver found for contraband {}", contraband.id());
            return Optional.empty();
        }
        final var newExpiresAt = LocalDateTime.now().plusHours(config.expirationHours());
        final var updated = contraband.withReceiver(receiver.get(), newExpiresAt);
        contrabandStorage.update(updated);
        logger.info("Assigned receiver {} to contraband {}", receiver.get(), contraband.id());
        return Optional.of(updated);
    }

    public void expire(Contraband contraband) {
        contrabandStorage.update(contraband.withStatus(ContrabandStatus.EXPIRED, LocalDateTime.now()));
        logger.info("Expired contraband {}", contraband.id());
    }

    public java.util.List<Contraband> findPendingForBlackMarket() {
        return contrabandStorage.findPendingForBlackMarket();
    }

    public java.util.List<Contraband> findExpired(LocalDateTime now) {
        return contrabandStorage.findExpired(now);
    }

    private ContrabandOpenResult generateReward(ContrabandTier tier, PersonageId personageId) {
        final var reward = config.rewards(tier).pick(RandomUtils::getWithMax);
        return applyReward(reward, personageId);
    }

    private ContrabandOpenResult.Success applyReward(ContrabandReward reward, PersonageId personageId) {
        return switch (reward) {
            case ContrabandReward.Gold gold -> {
                final var amount = RandomUtils.getInInterval(gold.amount());
                personageService.addMoney(personageId, new Money(amount));
                yield new ContrabandOpenResult.Success.Gold(new Money(amount));
            }
            case ContrabandReward.Energy energy -> {
                final var amount = RandomUtils.getInInterval(energy.amount());
                personageService.addEnergy(personageId, amount);
                yield new ContrabandOpenResult.Success.Energy(amount);
            }
            case ContrabandReward.Item item -> {
                final var rarity = item.rarityPicker().pick(RandomUtils::getWithMax);
                final var randomSlot = RandomUtils.getRandomElement(PersonageSlot.values());
                final var generatedItem = itemService.generateItemForPersonage(
                    personageService.getByIdForce(personageId),
                    new GenerateItemParams(rarity, randomSlot, rarityModifierCount(rarity))
                );
                yield generatedItem.fold(
                    _ -> new ContrabandOpenResult.Success.Gold(new Money(0)),
                    ContrabandOpenResult.Success.ItemReward::new
                );
            }
            case ContrabandReward.HealthBuff buff -> {
                final var percent = RandomUtils.getInInterval(buff.percent());
                final var hours = RandomUtils.getInInterval(buff.hours());
                final var effect = applyBuff(personageId, percent, hours, EffectCharacteristic.HEALTH);
                yield new ContrabandOpenResult.Success.Buff(effect);
            }
            case ContrabandReward.AttackBuff buff -> {
                final var percent = RandomUtils.getInInterval(buff.percent());
                final var hours = RandomUtils.getInInterval(buff.hours());
                final var effect = applyBuff(personageId, percent, hours, EffectCharacteristic.ATTACK);
                yield new ContrabandOpenResult.Success.Buff(effect);
            }
        };
    }

    private PersonageEffect applyBuff(PersonageId personageId, int percent, int hours, EffectCharacteristic characteristic) {
        final var effect = new PersonageEffect(
            new Effect.Multiplier(percent, characteristic),
            LocalDateTime.now().plusHours(hours)
        );
        personageService.addEffect(personageId, PersonageEffectType.CONTRABAND_BUFF, effect);
        return effect;
    }

    private ContrabandOpenResult applyPenalty(ContrabandTier tier, PersonageId personageId) {
        final var penalty = config.penalties(tier).pick(RandomUtils::getWithMax);
        return applyPenalty(penalty, personageId);
    }

    private ContrabandOpenResult.Failure applyPenalty(ContrabandPenalty penalty, PersonageId personageId) {
        return switch (penalty) {
            case ContrabandPenalty.HealthDebuff debuff -> {
                final var percent = RandomUtils.getInInterval(debuff.percent());
                final var hours = RandomUtils.getInInterval(debuff.hours());
                final var effect = applyDebuff(personageId, percent, hours, EffectCharacteristic.HEALTH);
                yield new ContrabandOpenResult.Failure.Debuff(effect);
            }
            case ContrabandPenalty.AttackDebuff debuff -> {
                final var percent = RandomUtils.getInInterval(debuff.percent());
                final var hours = RandomUtils.getInInterval(debuff.hours());
                final var effect = applyDebuff(personageId, percent, hours, EffectCharacteristic.ATTACK);
                yield new ContrabandOpenResult.Failure.Debuff(effect);
            }
            case ContrabandPenalty.GoldLoss loss -> {
                final var penaltyAmount = RandomUtils.getInInterval(loss.goldAmount());
                final var currentMoney = personageService.getByIdForce(personageId).money().value();
                final var actualLoss = Math.min(penaltyAmount, currentMoney);
                personageService.addMoney(personageId, new Money(-actualLoss));
                yield new ContrabandOpenResult.Failure.GoldLoss(actualLoss);
            }
            case ContrabandPenalty.Nothing() -> ContrabandOpenResult.Failure.Nothing.INSTANCE;
        };
    }

    private PersonageEffect applyDebuff(PersonageId personageId, int percent, int hours, EffectCharacteristic characteristic) {
        final var effect = new PersonageEffect(
            new Effect.MinusMultiplier(percent, characteristic),
            LocalDateTime.now().plusHours(hours)
        );
        personageService.addEffect(personageId, PersonageEffectType.CONTRABAND_DEBUFF, effect);
        return effect;
    }

    private int rarityModifierCount(ItemRarity rarity) {
        return switch (rarity) {
            case COMMON, UNCOMMON -> 0;
            case RARE -> 1;
            case EPIC, LEGENDARY -> 2;
        };
    }
}
