package ru.homyakin.seeker.game.shop;

import io.vavr.control.Either;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.homyakin.seeker.game.item.ItemService;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.item.models.PersonageItem;
import ru.homyakin.seeker.game.item.storm.StormEnhanceConfig;
import ru.homyakin.seeker.game.item.storm.StormEnhanceOutcomePicker;
import ru.homyakin.seeker.game.item.storm.StormEnhanceTechnicalLimit;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.shop.errors.AddModifierError;
import ru.homyakin.seeker.game.shop.errors.NoSuchItemAtPersonage;
import ru.homyakin.seeker.game.shop.errors.StormEnhanceError;
import ru.homyakin.seeker.game.shop.models.AvailableAction;
import ru.homyakin.seeker.game.shop.models.EnhanceAction;
import ru.homyakin.seeker.game.shop.models.EnhanceOutcome;
import ru.homyakin.seeker.game.shop.models.EnhanceResult;
import ru.homyakin.seeker.game.shop.models.StormEnhanceAction;
import ru.homyakin.seeker.game.shop.models.StormEnhanceResult;

import java.util.Optional;

@Service
public class EnhanceService {
    private final ItemService itemService;
    private final PersonageService personageService;
    private final ShopConfig config;
    private final StormEnhanceConfig stormEnhanceConfig;
    private final StormEnhanceTechnicalLimit stormEnhanceTechnicalLimit;
    private final StormEnhanceOutcomePicker stormEnhanceOutcomePicker;

    public EnhanceService(
        ItemService itemService,
        PersonageService personageService,
        ShopConfig config,
        StormEnhanceConfig stormEnhanceConfig,
        StormEnhanceTechnicalLimit stormEnhanceTechnicalLimit,
        StormEnhanceOutcomePicker stormEnhanceOutcomePicker
    ) {
        this.itemService = itemService;
        this.personageService = personageService;
        this.config = config;
        this.stormEnhanceConfig = stormEnhanceConfig;
        this.stormEnhanceTechnicalLimit = stormEnhanceTechnicalLimit;
        this.stormEnhanceOutcomePicker = stormEnhanceOutcomePicker;
    }

    public Either<NoSuchItemAtPersonage, AvailableAction> availableAction(PersonageId personageId, long itemId) {
        final var item = itemService.getPersonageItem(personageId, itemId);
        if (item.isEmpty()) {
            return Either.left(NoSuchItemAtPersonage.INSTANCE);
        }
        return Either.right(availableAction(item.get()));
    }

    public Either<AddModifierError, EnhanceResult> enhance(PersonageId personageId, long itemId) {
        final var item = itemService.getPersonageItem(personageId, itemId);
        if (item.isEmpty()) {
            return Either.left(AddModifierError.NoSuchItem.INSTANCE);
        }
        if (item.get().rarity().next().isEmpty()) {
            return Either.left(AddModifierError.MaxRarity.INSTANCE);
        }
        final var outcome = item.get().rarity() == ItemRarity.COMMON
            ? EnhanceOutcome.ADDED_MODIFIER
            : EnhanceOutcome.UPGRADED_RARITY;
        final var price = enhancePrice(item.get());
        final var takeMoneyResult = personageService.tryTakeMoney(personageId, price);
        if (takeMoneyResult.isLeft()) {
            return Either.left(new AddModifierError.NotEnoughMoney(price));
        }
        return itemService.enhance(item.get())
            .mapLeft(
                _ -> {
                    personageService.addMoney(personageId, price);
                    return (AddModifierError) AddModifierError.MaxRarity.INSTANCE;
                }
            )
            .map(enhanced -> new EnhanceResult(availableAction(enhanced), outcome));
    }

    @Transactional
    public Either<StormEnhanceError, StormEnhanceResult> stormEnhance(
        PersonageId personageId,
        long itemId,
        int expectedLevel,
        long expectedRevision
    ) {
        personageService.lockForItemChange(personageId);
        final var item = itemService.getPersonageItemForUpdate(personageId, itemId);
        if (item.isEmpty()) {
            return Either.left(StormEnhanceError.NoSuchItem.INSTANCE);
        }
        final var currentLevel = item.get().enhanceLevel();
        if (currentLevel != expectedLevel || item.get().enhanceRevision() != expectedRevision) {
            return Either.left(new StormEnhanceError.StaleItemState(currentLevel, item.get().enhanceRevision()));
        }
        final var maxLevel = stormEnhanceTechnicalLimit.maxStateLevel(item.get().object());
        if (currentLevel >= maxLevel || item.get().enhanceRevision() == Long.MAX_VALUE) {
            return Either.left(new StormEnhanceError.TechnicalLimitReached(maxLevel));
        }
        final var cost = stormEnhanceConfig.costForLevel(currentLevel, item.get().object().slots());
        final var probabilities = stormEnhanceConfig.probabilitiesForLevel(currentLevel);
        final var takeResult = personageService.tryTakeStormShards(personageId, cost);
        if (takeResult.isLeft()) {
            return Either.left(new StormEnhanceError.NotEnoughStormShards(cost));
        }
        final var outcome = stormEnhanceOutcomePicker.pick(probabilities);
        final var nextLevel = switch (outcome) {
            case SUCCESS -> Math.incrementExact(currentLevel);
            case FAILURE -> currentLevel;
            case ROLLBACK -> Math.max(0, currentLevel - 1);
        };
        final var enhanced = itemService.applyStormEnhance(item.get(), nextLevel);
        return Either.right(new StormEnhanceResult(availableAction(enhanced), outcome));
    }

    private AvailableAction availableAction(PersonageItem item) {
        final Optional<EnhanceAction> rarityAction = item.rarity() == ItemRarity.LEGENDARY
            ? Optional.empty()
            : Optional.of(new EnhanceAction.Enhance(enhancePrice(item)));
        final var maxLevel = stormEnhanceTechnicalLimit.maxStateLevel(item.object());
        final Optional<StormEnhanceAction> stormAction;
        if (item.enhanceLevel() >= maxLevel || item.enhanceRevision() == Long.MAX_VALUE) {
            stormAction = Optional.empty();
        } else {
            stormAction = Optional.of(new StormEnhanceAction(
                stormEnhanceConfig.costForLevel(item.enhanceLevel(), item.object().slots()),
                stormEnhanceConfig.probabilitiesForLevel(item.enhanceLevel()),
                item.enhanceLevel(),
                item.enhanceLevel() + 1,
                item.enhanceRevision()
            ));
        }
        return new AvailableAction(rarityAction, stormAction, item);
    }

    private Money enhancePrice(PersonageItem item) {
        final var nextRarity = item.rarity().next()
            .orElseThrow(() -> new IllegalStateException("Legendary items can't be enhanced"));
        final var basePrice = config.buyingPriceByRarity(nextRarity);
        return Money.from((int) (basePrice.value() * 1.5));
    }
}
