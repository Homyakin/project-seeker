package ru.homyakin.seeker.game.shop;

import io.vavr.control.Either;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.homyakin.seeker.game.item.ItemService;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.rarity.ItemRarity;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.shop.errors.BuyItemError;
import ru.homyakin.seeker.game.shop.errors.NoSuchItemAtPersonage;
import ru.homyakin.seeker.game.shop.models.ShopItem;
import ru.homyakin.seeker.game.shop.models.ShopItemType;
import ru.homyakin.seeker.game.shop.models.SoldItem;

import java.util.ArrayList;
import java.util.List;

@Service
public class ShopService {
    private final ItemService itemService;
    private final PersonageService personageService;
    private final ShopConfig config;

    public ShopService(ItemService itemService, PersonageService personageService, ShopConfig config) {
        this.itemService = itemService;
        this.personageService = personageService;
        this.config = config;
    }

    public List<ShopItem> getShopItems(PersonageId personageId) {
        final var items = new ArrayList<ShopItem>();
        final var personageItems = itemService.getPersonageItems(personageId);

        for (final var item: personageItems) {
            if (!item.isEquipped()) {
                items.add(new ShopItem.Sell(config.priceByRarity(item.rarity()), item));
            }
        }
        items.addAll(config.getBuyingItems());

        return items;
    }

    @Transactional
    public Either<BuyItemError, Item> buyItem(PersonageId personageId, ShopItemType type) {
        final var personage = personageService.getByIdForce(personageId);
        final var price = config.priceByType(type);
        if (personage.money().lessThan(config.priceByType(type))) {
            return Either.left(BuyItemError.NotEnoughMoney.INSTANCE);
        }
        final var personageWithTakenMoney = personageService.takeMoney(personage, price);

        final var result = switch (type) {
            case COMMON -> itemService.generateItemWithRarity(personageWithTakenMoney, ItemRarity.COMMON);
            case UNCOMMON -> itemService.generateItemWithRarity(personageWithTakenMoney, ItemRarity.UNCOMMON);
            case RARE -> itemService.generateItemWithRarity(personageWithTakenMoney, ItemRarity.RARE);
            case EPIC -> itemService.generateItemWithRarity(personageWithTakenMoney, ItemRarity.EPIC);
            case LEGENDARY -> itemService.generateItemWithRarity(personageWithTakenMoney, ItemRarity.LEGENDARY);
            case RANDOM -> itemService.generateItemForPersonage(personageWithTakenMoney);
        };
        return result.mapLeft(
            _ -> {
                personageService.addMoney(personage, price);
                return BuyItemError.NotEnoughSpaceInBag.INSTANCE;
            }
        );
    }

    @Transactional
    public Either<NoSuchItemAtPersonage, SoldItem> sellItem(PersonageId personageId, Long itemId) {
        final var personage = personageService.getByIdForce(personageId);
        return itemService.dropItem(personage, itemId)
            .mapLeft(_ -> NoSuchItemAtPersonage.INSTANCE)
            .map(item -> {
                final var price = config.priceByRarity(item.rarity());
                personageService.addMoney(personage, price);
                return new SoldItem(item, price);
            });
    }

}
