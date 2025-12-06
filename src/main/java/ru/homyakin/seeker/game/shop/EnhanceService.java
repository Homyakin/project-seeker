package ru.homyakin.seeker.game.shop;

import io.vavr.control.Either;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.item.ItemService;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.shop.errors.AddModifierError;
import ru.homyakin.seeker.game.shop.errors.NoSuchItemAtPersonage;
import ru.homyakin.seeker.game.shop.errors.RepairError;
import ru.homyakin.seeker.game.shop.models.AvailableAction;
import ru.homyakin.seeker.game.shop.models.EnhanceAction;

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

    public Either<AddModifierError, AvailableAction> addModifier(PersonageId personageId, long itemId) {
        final var item = itemService.getPersonageItem(personageId, itemId);
        if (item.isEmpty()) {
            return Either.left(AddModifierError.NoSuchItem.INSTANCE);
        }
        if (item.get().isBroken()) {
            return Either.left(AddModifierError.ItemIsBroken.INSTANCE);
        }
        final var price = addModifierPrice(item.get());
        final var takeMoneyResult = personageService.tryTakeMoney(personageId, price);
        if (takeMoneyResult.isLeft()) {
            return Either.left(new AddModifierError.NotEnoughMoney(price));
        }
        return itemService.addModifier(item.get())
            .mapLeft(
                _ -> {
                    personageService.addMoney(personageId, price);
                    return (AddModifierError) AddModifierError.MaxModifiers.INSTANCE;
                }
            )
            .map(this::availableAction);
    }

    public Either<RepairError, AvailableAction> repair(PersonageId personageId, long itemId) {
        final var item = itemService.getPersonageItem(personageId, itemId);
        if (item.isEmpty()) {
            return Either.left(RepairError.NoSuchItem.INSTANCE);
        }
        final var price = repairPrice(item.get());
        final var takeMoneyResult = personageService.tryTakeMoney(personageId, price);
        if (takeMoneyResult.isLeft()) {
            return Either.left(new RepairError.NotEnoughMoney(price));
        }
        return itemService.repair(item.get())
            .mapLeft(
                _ -> {
                    personageService.addMoney(personageId, price);
                    return (RepairError) RepairError.NotBroken.INSTANCE;
                }
            )
            .map(this::availableAction);
    }

    private AvailableAction availableAction(Item item) {
        if (item.isBroken()) {
            return new AvailableAction(
                Optional.of(new EnhanceAction.Repair(repairPrice(item))),
                item
            );
        }
        if (item.modifiers().size() == 2) {
            return new AvailableAction(Optional.empty(), item);
        }
        return new AvailableAction(
            Optional.of(
                new EnhanceAction.AddModifier(addModifierPrice(item))
            ),
            item
        );
    }

    private Money addModifierPrice(Item item) {
        final var basePrice = config.buyingPriceByRarity(item.rarity());
        final var multiplier = 2.0 + item.modifiers().size();
        return Money.from((int) (basePrice.value() * multiplier));
    }

    private Money repairPrice(Item item) {
        final var basePrice = config.buyingPriceByRarity(item.rarity());
        return Money.from(basePrice.value() + (int) (basePrice.value() * item.modifiers().size() * 1.5));
    }
}
