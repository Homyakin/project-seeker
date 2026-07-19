package ru.homyakin.seeker.game.item.loadout.action;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import ru.homyakin.seeker.game.item.ItemService;
import ru.homyakin.seeker.game.item.database.ItemDao;
import ru.homyakin.seeker.game.item.loadout.entity.ApplyLoadoutError;
import ru.homyakin.seeker.game.item.loadout.entity.CreateLoadoutError;
import ru.homyakin.seeker.game.item.loadout.entity.EquipmentLoadout;
import ru.homyakin.seeker.game.item.loadout.entity.SaveLoadoutError;
import ru.homyakin.seeker.game.item.loadout.infra.postgres.EquipmentLoadoutDao;
import ru.homyakin.seeker.game.item.models.Inventory;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.item.models.PersonageItem;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.test_utils.CatalogTestUtils;
import ru.homyakin.seeker.test_utils.PersonageUtils;

class EquipmentLoadoutServiceTest {
    private final EquipmentLoadoutDao loadoutDao = Mockito.mock(EquipmentLoadoutDao.class);
    private final ItemService itemService = Mockito.mock(ItemService.class);
    private final ItemDao itemDao = Mockito.mock(ItemDao.class);
    private final EquipmentLoadoutService service = new EquipmentLoadoutService(loadoutDao, itemService, itemDao);

    @Test
    void createFromCurrent_requiresValidName() {
        final var personageId = PersonageUtils.random().id();
        final var result = service.createFromCurrent(personageId, "");
        Assertions.assertTrue(result.isLeft());
        Assertions.assertInstanceOf(CreateLoadoutError.InvalidName.class, result.getLeft());
    }

    @Test
    void createFromCurrent_rejectsWhenMaxReached() {
        final var personageId = PersonageUtils.random().id();
        Mockito.when(loadoutDao.countByPersonageId(personageId)).thenReturn(3);

        final var result = service.createFromCurrent(personageId, "Raid");

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(CreateLoadoutError.MaxLoadoutsReached.INSTANCE, result.getLeft());
    }

    @Test
    void createFromCurrent_savesEquippedItemIds() {
        final var personageId = PersonageUtils.random().id();
        final var equipped = personageItem(1L, personageId, true, PersonageSlot.MAIN_HAND);
        final var bag = personageItem(2L, personageId, false, PersonageSlot.BODY);
        final var created = new EquipmentLoadout(10L, personageId, "Raid", List.of(1L));

        Mockito.when(loadoutDao.countByPersonageId(personageId)).thenReturn(0);
        Mockito.when(itemService.getPersonageItems(personageId)).thenReturn(new Inventory(List.of(equipped, bag)));
        Mockito.when(loadoutDao.insert(personageId, "Raid", List.of(1L))).thenReturn(10L);
        Mockito.when(loadoutDao.findById(10L)).thenReturn(Optional.of(created));

        final var result = service.createFromCurrent(personageId, "Raid");

        Assertions.assertTrue(result.isRight());
        Assertions.assertEquals(created, result.get());
        Mockito.verify(loadoutDao).insert(personageId, "Raid", List.of(1L));
    }

    @Test
    void saveCurrent_updatesItemIds() {
        final var personageId = PersonageUtils.random().id();
        final var loadout = new EquipmentLoadout(5L, personageId, "Old", List.of(1L));
        final var equipped = personageItem(3L, personageId, true, PersonageSlot.HELMET);
        final var updated = new EquipmentLoadout(5L, personageId, "Old", List.of(3L));

        Mockito.when(loadoutDao.findById(5L)).thenReturn(Optional.of(loadout), Optional.of(updated));
        Mockito.when(itemService.getPersonageItems(personageId)).thenReturn(new Inventory(List.of(equipped)));

        final var result = service.saveCurrent(personageId, 5L);

        Assertions.assertTrue(result.isRight());
        Mockito.verify(loadoutDao).updateItemIds(5L, List.of(3L));
    }

    @Test
    void saveCurrent_notFoundForOtherPersonage() {
        final var personageId = PersonageUtils.random().id();
        final var other = PersonageUtils.random().id();
        Mockito.when(loadoutDao.findById(5L))
            .thenReturn(Optional.of(new EquipmentLoadout(5L, other, "X", List.of())));

        final var result = service.saveCurrent(personageId, 5L);

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(SaveLoadoutError.LoadoutNotFound.INSTANCE, result.getLeft());
    }

    @Test
    void apply_failsWhenItemsMissing() {
        final var personageId = PersonageUtils.random().id();
        final var loadout = new EquipmentLoadout(1L, personageId, "Raid", List.of(10L, 11L));
        final var owned = personageItem(10L, personageId, false, PersonageSlot.MAIN_HAND);

        Mockito.when(loadoutDao.findById(1L)).thenReturn(Optional.of(loadout));
        Mockito.when(itemService.getPersonageItems(personageId)).thenReturn(new Inventory(List.of(owned)));

        final var result = service.apply(personageId, 1L);

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(List.of(11L), ((ApplyLoadoutError.MissingItems) result.getLeft()).missingItemIds());
        Mockito.verify(itemDao, Mockito.never()).setEquippedForPersonage(Mockito.any(), Mockito.any());
    }

    @Test
    void apply_failsWhenBagWouldOverflow() {
        final var personageId = PersonageUtils.random().id();
        final var loadout = new EquipmentLoadout(1L, personageId, "Raid", List.of());
        // totalOwned=16, targetCount=0 -> unequipped=16 > 15
        final var bagOnly = new ArrayList<PersonageItem>();
        for (long i = 1; i <= 16; i++) {
            bagOnly.add(personageItem(i, personageId, false, PersonageSlot.BODY));
        }

        Mockito.when(loadoutDao.findById(1L)).thenReturn(Optional.of(loadout));
        Mockito.when(itemService.getPersonageItems(personageId)).thenReturn(new Inventory(bagOnly));

        final var result = service.apply(personageId, 1L);

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(ApplyLoadoutError.NotEnoughSpaceInBag.INSTANCE, result.getLeft());
    }

    @Test
    void apply_setsEquippedAtomically() {
        final var personageId = PersonageUtils.random().id();
        final var loadout = new EquipmentLoadout(1L, personageId, "Raid", List.of(2L));
        final var item1 = personageItem(1L, personageId, true, PersonageSlot.MAIN_HAND);
        final var item2 = personageItem(2L, personageId, false, PersonageSlot.BODY);

        Mockito.when(loadoutDao.findById(1L)).thenReturn(Optional.of(loadout));
        Mockito.when(itemService.getPersonageItems(personageId)).thenReturn(new Inventory(List.of(item1, item2)));

        final var result = service.apply(personageId, 1L);

        Assertions.assertTrue(result.isRight());
        Mockito.verify(itemDao).setEquippedForPersonage(personageId, List.of(2L));
    }

    @Test
    void rename_updatesName() {
        final var personageId = PersonageUtils.random().id();
        final var loadout = new EquipmentLoadout(1L, personageId, "Old", List.of());
        final var renamed = new EquipmentLoadout(1L, personageId, "New", List.of());

        Mockito.when(loadoutDao.findById(1L)).thenReturn(Optional.of(loadout), Optional.of(renamed));

        final var result = service.rename(personageId, 1L, "New");

        Assertions.assertTrue(result.isRight());
        Assertions.assertEquals("New", result.get().name());
        Mockito.verify(loadoutDao).updateName(1L, "New");
    }

    @Test
    void delete_removesLoadout() {
        final var personageId = PersonageUtils.random().id();
        Mockito.when(loadoutDao.findById(1L))
            .thenReturn(Optional.of(new EquipmentLoadout(1L, personageId, "Raid", List.of())));

        final var result = service.delete(personageId, 1L);

        Assertions.assertTrue(result.isRight());
        Mockito.verify(loadoutDao).delete(1L);
    }

    @Test
    void removeItemFromLoadouts_prunesIdsAndReturnsNames() {
        final var personageId = PersonageUtils.random().id();
        final var loadout1 = new EquipmentLoadout(1L, personageId, "A", List.of(10L, 11L));
        final var loadout2 = new EquipmentLoadout(2L, personageId, "B", List.of(10L));

        Mockito.when(loadoutDao.findByPersonageIdAndItemId(personageId, 10L))
            .thenReturn(List.of(loadout1, loadout2));

        final var names = service.removeItemFromLoadouts(personageId, 10L);

        Assertions.assertEquals(List.of("A", "B"), names);
        @SuppressWarnings("unchecked")
        final ArgumentCaptor<List<Long>> idsCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(loadoutDao).updateItemIds(Mockito.eq(1L), idsCaptor.capture());
        Assertions.assertEquals(List.of(11L), idsCaptor.getValue());
        Mockito.verify(loadoutDao).updateItemIds(2L, List.of());
    }

    private PersonageItem personageItem(long id, PersonageId personageId, boolean equipped, PersonageSlot... slots) {
        return new PersonageItem(
            id,
            1,
            CatalogTestUtils.itemObject(slots),
            Optional.empty(),
            Optional.empty(),
            ItemRarity.COMMON,
            Optional.of(personageId),
            equipped
        );
    }
}
