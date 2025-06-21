package ru.homyakin.seeker.game.shop;

import io.vavr.control.Either;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.homyakin.seeker.game.item.ItemService;
import ru.homyakin.seeker.game.item.models.GenerateItemParams;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.random.item.action.PersonageNextShopItemParams;
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
    private final PersonageNextShopItemParams personageNextShopItemParams;
    private final ShopConfig config;

    public ShopService(
        ItemService itemService,
        PersonageService personageService,
        PersonageNextShopItemParams personageNextShopItemParams,
        ShopConfig config
    ) {
        this.itemService = itemService;
        this.personageService = personageService;
        this.personageNextShopItemParams = personageNextShopItemParams;
        this.config = config;
    }

    public List<ShopItem> getShopItems(PersonageId personageId) {
        final var items = new ArrayList<ShopItem>();
        final var personageItems = itemService.getPersonageItems(personageId);

        for (final var item : personageItems) {
            if (!item.isEquipped()) {
                items.add(new ShopItem.Sell(config.sellingPriceByItem(item), item));
            }
        }
        items.addAll(config.getBuyingItems());

        return items;
    }

    @Transactional
    public Either<BuyItemError, Item> buyItem(PersonageId personageId, ShopItemType type) {
        final var personage = personageService.getByIdForce(personageId);
        final var price = config.buyingPriceByType(type);
        if (personage.money().lessThan(price)) {
            return Either.left(new BuyItemError.NotEnoughMoney(price));
        }
        final var personageWithTakenMoney = personageService.takeMoney(personage, price);

        final var params = personageNextShopItemParams.getForShopItemType(personageId, type);
        final var result = itemService.generateItemForPersonage(
            personageWithTakenMoney,
            new GenerateItemParams(
                params.rarity(),
                params.slot(),
                params.modifiersCount()
            )
        );
        return result.mapLeft(
            _ -> {
                personageService.addMoney(personage, price);
                return BuyItemError.NotEnoughSpaceInBag.INSTANCE;
            }
        );
    }

    @Transactional
    public Either<NoSuchItemAtPersonage, SoldItem> sellItem(PersonageId personageId, Long itemId) {
        final var removeResult = itemService.removeItem(personageId, itemId);
        if (removeResult.isEmpty()) {
            return Either.left(NoSuchItemAtPersonage.INSTANCE);
        }
        final var item = removeResult.get();
        final var price = config.sellingPriceByItem(item);
        personageService.addMoney(personageId, price);
        return Either.right(new SoldItem(item, price));
    }

}
