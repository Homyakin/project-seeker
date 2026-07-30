package ru.homyakin.seeker.game.shop;

import io.vavr.control.Either;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.item.ItemService;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.item.models.PersonageItem;
import ru.homyakin.seeker.game.item.storm.StormEnhanceConfig;
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
import ru.homyakin.seeker.game.shop.models.StormEnhanceOutcome;
import ru.homyakin.seeker.game.shop.models.StormEnhanceResult;
import ru.homyakin.seeker.utils.RandomUtils;

import java.util.Optional;

@Service
public class EnhanceService {
    private final ItemService itemService;
    private final PersonageService personageService;
    private final ShopConfig config;
    private final StormEnhanceConfig stormEnhanceConfig;

    public EnhanceService(
        ItemService itemService,
        PersonageService personageService,
        ShopConfig config,
        StormEnhanceConfig stormEnhanceConfig
    ) {
        this.itemService = itemService;
        this.personageService = personageService;
        this.config = config;
        this.stormEnhanceConfig = stormEnhanceConfig;
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

    public Either<StormEnhanceError, StormEnhanceResult> stormEnhance(PersonageId personageId, long itemId) {
        final var item = itemService.getPersonageItem(personageId, itemId);
        if (item.isEmpty()) {
            return Either.left(StormEnhanceError.NoSuchItem.INSTANCE);
        }
        if (item.get().enhanceLevel() >= stormEnhanceConfig.maxLevel()) {
            return Either.left(StormEnhanceError.MaxLevel.INSTANCE);
        }
        final var cost = stormEnhanceConfig.costForLevel(item.get().enhanceLevel());
        final var successPercent = stormEnhanceConfig.successPercentForLevel(item.get().enhanceLevel());
        final var takeResult = personageService.tryTakeStormShards(personageId, cost);
        if (takeResult.isLeft()) {
            return Either.left(new StormEnhanceError.NotEnoughStormShards(cost));
        }
        final boolean success = RandomUtils.processChance(successPercent);
        if (success) {
            final var enhanced = itemService.stormEnhance(item.get());
            return Either.right(new StormEnhanceResult(availableAction(enhanced), StormEnhanceOutcome.SUCCESS));
        }
        return Either.right(new StormEnhanceResult(availableAction(item.get()), StormEnhanceOutcome.FAILURE));
    }

    private AvailableAction availableAction(PersonageItem item) {
        final Optional<EnhanceAction> rarityAction = item.rarity() == ItemRarity.LEGENDARY
            ? Optional.empty()
            : Optional.of(new EnhanceAction.Enhance(enhancePrice(item)));
        final Optional<StormEnhanceAction> stormAction = item.enhanceLevel() >= stormEnhanceConfig.maxLevel()
            ? Optional.empty()
            : Optional.of(new StormEnhanceAction(
                stormEnhanceConfig.costForLevel(item.enhanceLevel()),
                stormEnhanceConfig.successPercentForLevel(item.enhanceLevel()),
                item.enhanceLevel(),
                item.enhanceLevel() + 1
            ));
        return new AvailableAction(rarityAction, stormAction, item);
    }

    private Money enhancePrice(PersonageItem item) {
        final var nextRarity = item.rarity().next()
            .orElseThrow(() -> new IllegalStateException("Legendary items can't be enhanced"));
        final var basePrice = config.buyingPriceByRarity(nextRarity);
        return Money.from((int) (basePrice.value() * 1.5));
    }
}
