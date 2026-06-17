package ru.homyakin.seeker.game.item;

import io.vavr.control.Either;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import ru.homyakin.seeker.game.item.database.ItemDao;
import ru.homyakin.seeker.game.item.database.ItemObjectDao;
import ru.homyakin.seeker.game.item.errors.GenerateItemError;
import ru.homyakin.seeker.game.item.models.CatalogItemObject;
import ru.homyakin.seeker.game.item.models.DefaultItems;
import ru.homyakin.seeker.game.item.models.GenerateItemParams;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.item.models.PersonageItem;
import ru.homyakin.seeker.game.item.modifier.ItemModifierService;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.test_utils.CatalogTestUtils;
import ru.homyakin.seeker.test_utils.PersonageUtils;

import java.util.Collections;
import java.util.Optional;

class ItemServiceGenerateItemTest {
    private final ItemObjectDao itemObjectDao = Mockito.mock(ItemObjectDao.class);
    private final ItemModifierService itemModifierService = Mockito.mock(ItemModifierService.class);
    private final ItemDao itemDao = Mockito.mock(ItemDao.class);
    private final ItemService itemService = new ItemService(itemObjectDao, itemModifierService, itemDao);

    @Test
    void generateItemForPersonage_fromCatalogObject_createsCommonItemWithoutModifier() {
        final var personage = PersonageUtils.random();
        final var catalogObject = new CatalogItemObject(7, DefaultItems.MAIN_FIST.object());
        final var savedItem = new PersonageItem(
            42L,
            7,
            catalogObject.object(),
            Optional.empty(),
            Optional.empty(),
            ItemRarity.COMMON,
            Optional.of(personage.id()),
            false
        );

        Mockito.when(itemDao.getByPersonageId(personage.id())).thenReturn(Collections.emptyList());
        Mockito.when(itemDao.save(Mockito.any())).thenReturn(42L);
        Mockito.when(itemDao.getById(42L)).thenReturn(Optional.of(savedItem));

        final var result = itemService.generateItemForPersonage(personage, catalogObject);

        Assertions.assertTrue(result.isRight());
        Assertions.assertEquals(savedItem, result.get());

        final var captor = ArgumentCaptor.forClass(PersonageItem.class);
        Mockito.verify(itemDao).save(captor.capture());
        final var itemToSave = captor.getValue();
        Assertions.assertEquals(ItemRarity.COMMON, itemToSave.rarity());
        Assertions.assertTrue(itemToSave.modifier().isEmpty());
        Assertions.assertEquals(7, itemToSave.objectId());
        Assertions.assertEquals(catalogObject.object(), itemToSave.object());
    }

    @Test
    void generateItemForPersonage_fromParams_picksModifierForNonCommonRarity() {
        final var personage = PersonageUtils.random();
        final var catalogObject = CatalogTestUtils.catalogObject(3, PersonageSlot.MAIN_HAND);
        final var catalogModifier = CatalogTestUtils.catalogModifier(5);
        final var params = new GenerateItemParams(ItemRarity.RARE, PersonageSlot.MAIN_HAND);
        final var savedItem = Mockito.mock(PersonageItem.class);

        Mockito.when(itemObjectDao.getRandomObject(PersonageSlot.MAIN_HAND)).thenReturn(catalogObject);
        Mockito.when(itemModifierService.pickModifier(ItemRarity.RARE, catalogObject.object(), PersonageSlot.MAIN_HAND))
            .thenReturn(Optional.of(catalogModifier));
        Mockito.when(itemDao.getByPersonageId(personage.id())).thenReturn(Collections.emptyList());
        Mockito.when(itemDao.save(Mockito.any())).thenReturn(10L);
        Mockito.when(itemDao.getById(10L)).thenReturn(Optional.of(savedItem));

        itemService.generateItemForPersonage(personage, params);

        final var captor = ArgumentCaptor.forClass(PersonageItem.class);
        Mockito.verify(itemDao).save(captor.capture());
        final var itemToSave = captor.getValue();
        Assertions.assertEquals(ItemRarity.RARE, itemToSave.rarity());
        Assertions.assertEquals(Optional.of(5), itemToSave.modifierId());
        Assertions.assertEquals(Optional.of(catalogModifier.modifier()), itemToSave.modifier());
    }

    @Test
    void generateItemForPersonage_whenBagFull_returnsNotEnoughSpaceError() {
        final var personage = PersonageUtils.random();
        final var catalogObject = CatalogTestUtils.catalogObject(1, PersonageSlot.MAIN_HAND);
        final var bagItem = Mockito.mock(PersonageItem.class);
        Mockito.when(bagItem.isEquipped()).thenReturn(false);

        Mockito.when(itemDao.getByPersonageId(personage.id())).thenReturn(Collections.nCopies(15, bagItem));
        Mockito.when(itemDao.save(Mockito.any())).thenReturn(99L);
        Mockito.when(itemDao.getById(99L)).thenReturn(Optional.of(Mockito.mock(PersonageItem.class)));

        final var result = itemService.generateItemForPersonage(personage, catalogObject);

        Assertions.assertTrue(result.isLeft());
        Assertions.assertInstanceOf(GenerateItemError.NotEnoughSpace.class, result.getLeft());
    }

}
