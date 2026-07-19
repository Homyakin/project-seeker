package ru.homyakin.seeker.game.shop;

import io.vavr.control.Either;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.homyakin.seeker.game.item.ItemService;
import ru.homyakin.seeker.game.item.database.ItemObjectDao;
import ru.homyakin.seeker.game.item.loadout.action.EquipmentLoadoutService;
import ru.homyakin.seeker.game.item.loadout.entity.EquipmentLoadout;
import ru.homyakin.seeker.game.item.models.GenerateItemParams;
import ru.homyakin.seeker.game.item.models.CatalogItemObject;
import ru.homyakin.seeker.game.item.models.PersonageItem;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.game.random.item.action.PersonageNextShopItemParams;
import ru.homyakin.seeker.game.shop.errors.BuyItemError;
import ru.homyakin.seeker.game.shop.errors.NoSuchItemAtPersonage;
import ru.homyakin.seeker.game.shop.models.ShopItem;
import ru.homyakin.seeker.game.shop.models.ShopItemType;
import ru.homyakin.seeker.game.shop.models.SoldItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ShopService {
    private final ItemService itemService;
    private final ItemObjectDao itemObjectDao;
    private final PersonageService personageService;
    private final PersonageNextShopItemParams personageNextShopItemParams;
    private final ShopConfig config;
    private final EquipmentLoadoutService equipmentLoadoutService;

    public ShopService(
        ItemService itemService,
        ItemObjectDao itemObjectDao,
        PersonageService personageService,
        PersonageNextShopItemParams personageNextShopItemParams,
        ShopConfig config,
        EquipmentLoadoutService equipmentLoadoutService
    ) {
        this.itemService = itemService;
        this.itemObjectDao = itemObjectDao;
        this.personageService = personageService;
        this.personageNextShopItemParams = personageNextShopItemParams;
        this.config = config;
        this.equipmentLoadoutService = equipmentLoadoutService;
    }

    public List<CatalogItemObject> getItemObjectsForSlot(PersonageSlot slot) {
        return itemObjectDao.listBySlot(slot);
    }

    public Money specificObjectUnitPrice() {
        return config.specificObjectUnitPrice();
    }

    public List<ShopItem> getShopItems(PersonageId personageId) {
        final var items = new ArrayList<ShopItem>();
        final var personageItems = itemService.getPersonageItems(personageId);

        for (final var item : personageItems.items()) {
            if (!item.isEquipped()) {
                items.add(new ShopItem.Sell(config.sellingPriceByItem(item), item));
            }
        }
        items.addAll(config.getBuyingItems());

        return items;
    }

    @Transactional
    public Either<BuyItemError, PersonageItem> buyItem(PersonageId personageId, ShopItemType type) {
        final var personage = personageService.getByIdForce(personageId);
        final var price = config.buyingPriceByType(type);
        if (personage.money().lessThan(price)) {
            return Either.left(new BuyItemError.NotEnoughMoney(price));
        }
        final var personageWithTakenMoney = personageService.takeMoney(personage, price);

        final var params = personageNextShopItemParams.getForShopItemType(personageId, type);
        final var result = itemService.generateItemForPersonage(
            personageWithTakenMoney,
            new GenerateItemParams(params.rarity(), params.slot())
        );
        return result.mapLeft(
            _ -> {
                personageService.addMoney(personageWithTakenMoney, price);
                return BuyItemError.NotEnoughSpaceInBag.INSTANCE;
            }
        );
    }

    @Transactional
    public Either<BuyItemError, PersonageItem> buyItemWithObject(PersonageId personageId, int objectId) {
        final var catalogObject = itemObjectDao.getById(objectId);
        if (catalogObject.isEmpty()) {
            return Either.left(BuyItemError.InvalidItemObject.INSTANCE);
        }
        return buyItemWithObject(personageId, catalogObject.get());
    }

    private Either<BuyItemError, PersonageItem> buyItemWithObject(
        PersonageId personageId,
        CatalogItemObject catalogObject
    ) {
        final var personage = personageService.getByIdForce(personageId);
        final var price = config.specificObjectBuyPrice(catalogObject.object().slots().size());
        if (personage.money().lessThan(price)) {
            return Either.left(new BuyItemError.NotEnoughMoney(price));
        }
        final var personageWithTakenMoney = personageService.takeMoney(personage, price);
        final var result = itemService.generateItemForPersonage(
            personageWithTakenMoney,
            catalogObject
        );
        return result.mapLeft(
            _ -> {
                personageService.addMoney(personageWithTakenMoney, price);
                return BuyItemError.NotEnoughSpaceInBag.INSTANCE;
            }
        );
    }

    public List<String> loadoutNamesForItem(PersonageId personageId, long itemId) {
        return equipmentLoadoutService.findByItemId(personageId, itemId).stream()
            .map(EquipmentLoadout::name)
            .toList();
    }

    public Optional<PersonageItem> getSellableItem(PersonageId personageId, long itemId) {
        return itemService.getPersonageItem(personageId, itemId)
            .filter(item -> !item.isEquipped());
    }

    public Money sellingPrice(PersonageItem item) {
        return config.sellingPriceByItem(item);
    }

    @Transactional
    public Either<NoSuchItemAtPersonage, SoldItem> sellItem(PersonageId personageId, Long itemId) {
        final var removeResult = itemService.removeItem(personageId, itemId);
        if (removeResult.isEmpty()) {
            return Either.left(NoSuchItemAtPersonage.INSTANCE);
        }
        final var item = removeResult.get();
        final var affectedLoadoutNames = equipmentLoadoutService.removeItemFromLoadouts(personageId, itemId);
        final var price = config.sellingPriceByItem(item);
        personageService.addMoney(personageId, price);
        return Either.right(new SoldItem(item, price, affectedLoadoutNames));
    }
}
