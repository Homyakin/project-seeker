package ru.homyakin.seeker.game.shop;

import io.vavr.control.Either;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import ru.homyakin.seeker.game.item.ItemService;
import ru.homyakin.seeker.game.item.database.ItemObjectDao;
import ru.homyakin.seeker.game.item.errors.GenerateItemError;
import ru.homyakin.seeker.game.item.loadout.action.EquipmentLoadoutService;
import ru.homyakin.seeker.game.item.models.CatalogItemObject;
import ru.homyakin.seeker.game.item.models.PersonageItem;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.game.random.item.action.PersonageNextShopItemParams;
import ru.homyakin.seeker.game.shop.errors.BuyItemError;
import ru.homyakin.seeker.test_utils.CatalogTestUtils;
import ru.homyakin.seeker.test_utils.PersonageUtils;

import java.util.List;
import java.util.Optional;

class ShopServiceTest {
    private final ItemService itemService = Mockito.mock(ItemService.class);
    private final ItemObjectDao itemObjectDao = Mockito.mock(ItemObjectDao.class);
    private final PersonageService personageService = Mockito.mock(PersonageService.class);
    private final PersonageNextShopItemParams personageNextShopItemParams = Mockito.mock(PersonageNextShopItemParams.class);
    private final EquipmentLoadoutService equipmentLoadoutService = Mockito.mock(EquipmentLoadoutService.class);
    private final ShopConfig config = new ShopConfig();
    private final ShopService shopService = new ShopService(
        itemService,
        itemObjectDao,
        personageService,
        personageNextShopItemParams,
        config,
        equipmentLoadoutService
    );

    private final PersonageId personageId = PersonageId.from(1);

    @BeforeEach
    void setUp() {
        config.setCommonPrice(50);
        config.setSellDiscountDivider(2);
    }

    @Test
    void buyItemWithObject_whenObjectMissing_returnsInvalidItemObject() {
        Mockito.when(itemObjectDao.getById(99)).thenReturn(Optional.empty());

        final var result = shopService.buyItemWithObject(personageId, 99);

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(BuyItemError.InvalidItemObject.INSTANCE, result.getLeft());
    }

    @Test
    void buyItemWithObject_whenNotEnoughMoney_returnsRequiredPrice() {
        final var catalogObject = CatalogTestUtils.catalogObject(1, PersonageSlot.MAIN_HAND);
        Mockito.when(itemObjectDao.getById(1)).thenReturn(Optional.of(catalogObject));
        Mockito.when(personageService.getByIdForce(personageId))
            .thenReturn(PersonageUtils.withId(personageId).addMoney(Money.from(50)));

        final var result = shopService.buyItemWithObject(personageId, 1);

        Assertions.assertTrue(result.isLeft());
        final var error = (BuyItemError.NotEnoughMoney) result.getLeft();
        Assertions.assertEquals(100, error.required().value());
    }

    @Test
    void buyItemWithObject_chargesUnitPriceTimesSlotCount() {
        final var catalogObject = CatalogTestUtils.catalogObject(
            2,
            PersonageSlot.MAIN_HAND,
            PersonageSlot.OFF_HAND
        );
        final var personage = PersonageUtils.withId(personageId).addMoney(Money.from(500));
        final var expectedItem = Mockito.mock(PersonageItem.class);

        Mockito.when(itemObjectDao.getById(2)).thenReturn(Optional.of(catalogObject));
        Mockito.when(personageService.getByIdForce(personageId)).thenReturn(personage);
        Mockito.when(personageService.takeMoney(personage, Money.from(200))).thenReturn(personage);
        Mockito.when(itemService.generateItemForPersonage(Mockito.eq(personage), Mockito.eq(catalogObject)))
            .thenReturn(Either.right(expectedItem));

        final var result = shopService.buyItemWithObject(personageId, 2);

        Assertions.assertTrue(result.isRight());
        Mockito.verify(personageService).takeMoney(personage, Money.from(200));
        Mockito.verify(itemService).generateItemForPersonage(personage, catalogObject);
    }

    @Test
    void buyItemWithObject_whenBagFull_refundsMoney() {
        final var catalogObject = CatalogTestUtils.catalogObject(1, PersonageSlot.MAIN_HAND);
        final var personage = PersonageUtils.withId(personageId).addMoney(Money.from(500));
        final var personageAfterPay = personage.addMoney(Money.from(-100));

        Mockito.when(itemObjectDao.getById(1)).thenReturn(Optional.of(catalogObject));
        Mockito.when(personageService.getByIdForce(personageId)).thenReturn(personage);
        Mockito.when(personageService.takeMoney(personage, Money.from(100))).thenReturn(personageAfterPay);
        Mockito.when(itemService.generateItemForPersonage(personageAfterPay, catalogObject))
            .thenReturn(Either.left(Mockito.mock(GenerateItemError.NotEnoughSpace.class)));

        final var result = shopService.buyItemWithObject(personageId, 1);

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(BuyItemError.NotEnoughSpaceInBag.INSTANCE, result.getLeft());
        Mockito.verify(personageService).addMoney(personageAfterPay, Money.from(100));
    }

    @Test
    void buyItemWithObject_onSuccess_passesCatalogObjectToItemService() {
        final var catalogObject = CatalogTestUtils.catalogObject(1, PersonageSlot.MAIN_HAND);
        final var personage = PersonageUtils.withId(personageId).addMoney(Money.from(500));
        final var expectedItem = Mockito.mock(PersonageItem.class);

        Mockito.when(itemObjectDao.getById(1)).thenReturn(Optional.of(catalogObject));
        Mockito.when(personageService.getByIdForce(personageId)).thenReturn(personage);
        Mockito.when(personageService.takeMoney(Mockito.any(), Mockito.any())).thenReturn(personage);
        Mockito.when(itemService.generateItemForPersonage(Mockito.any(), Mockito.any(CatalogItemObject.class)))
            .thenReturn(Either.right(expectedItem));

        shopService.buyItemWithObject(personageId, 1);

        final var catalogCaptor = ArgumentCaptor.forClass(CatalogItemObject.class);
        Mockito.verify(itemService).generateItemForPersonage(Mockito.eq(personage), catalogCaptor.capture());
        Assertions.assertEquals(catalogObject, catalogCaptor.getValue());
    }

    @Test
    void sellItem_prunesItemFromLoadoutsAndReturnsNames() {
        final var item = Mockito.mock(PersonageItem.class);
        Mockito.when(itemService.removeItem(personageId, 7L)).thenReturn(Optional.of(item));
        Mockito.when(equipmentLoadoutService.removeItemFromLoadouts(personageId, 7L))
            .thenReturn(List.of("Raid", "PvP"));
        Mockito.when(item.rarity()).thenReturn(ru.homyakin.seeker.game.item.models.ItemRarity.COMMON);

        final var result = shopService.sellItem(personageId, 7L);

        Assertions.assertTrue(result.isRight());
        Assertions.assertEquals(List.of("Raid", "PvP"), result.get().affectedLoadoutNames());
        Mockito.verify(equipmentLoadoutService).removeItemFromLoadouts(personageId, 7L);
    }
}
