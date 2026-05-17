package ru.homyakin.seeker.game.shop;

import io.vavr.control.Either;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.item.ItemService;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.item.models.PersonageItem;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.shop.errors.AddModifierError;
import ru.homyakin.seeker.game.shop.errors.NoSuchItemAtPersonage;
import ru.homyakin.seeker.game.shop.models.AvailableAction;
import ru.homyakin.seeker.game.shop.models.EnhanceAction;
import ru.homyakin.seeker.game.shop.models.EnhanceOutcome;
import ru.homyakin.seeker.game.shop.models.EnhanceResult;

import java.util.Optional;

@Service
public class EnhanceService {
    private final ItemService itemService;
    private final PersonageService personageService;
    private final ShopConfig config;

    public EnhanceService(ItemService itemService, PersonageService personageService, ShopConfig config) {
        this.itemService = itemService;
        this.personageService = personageService;
        this.config = config;
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

    private AvailableAction availableAction(PersonageItem item) {
        if (item.rarity() == ItemRarity.LEGENDARY) {
            return new AvailableAction(Optional.empty(), item);
        }
        return new AvailableAction(
            Optional.of(new EnhanceAction.Enhance(enhancePrice(item))),
            item
        );
    }

    private Money enhancePrice(PersonageItem item) {
        final var basePrice = config.buyingPriceByRarity(item.rarity());
        final var multiplier = 1.5 + (item.modifier().isPresent() ? 1 : 0);
        final var slotSize = Math.max(1, item.object().slots().size());
        return Money.from((int) (basePrice.value() * multiplier * slotSize));
    }
}
