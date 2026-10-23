package ru.homyakin.seeker.game.item;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogLoader;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogVersion;
import ru.homyakin.seeker.game.item.catalog.EquipmentReleaseConfig;
import ru.homyakin.seeker.game.item.database.ItemCatalogLifecycleDao;
import ru.homyakin.seeker.game.item.database.ItemDao;
import ru.homyakin.seeker.game.item.models.DefaultItems;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.item.models.PersonageItem;
import ru.homyakin.seeker.game.personage.models.Characteristics;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;

class ItemServiceReleaseDefaultsTest {
    @Test
    void historicalConstructorKeepsLegacyItemsAndCharacteristics() {
        final var personageId = PersonageId.from(1);
        final var dao = Mockito.mock(ItemDao.class);
        Mockito.when(dao.getEquippedByPersonageIds(Set.of(personageId))).thenReturn(Map.of());
        final var service = new ItemService(null, null, dao, null, null);
        final var items = service.getEquippedItemsByPersonageIds(Set.of(personageId)).get(personageId);

        Assertions.assertAll(
            () -> Assertions.assertEquals(7, items.size()),
            () -> Assertions.assertTrue(items.contains(DefaultItems.MAIN_FIST)),
            () -> Assertions.assertTrue(items.contains(DefaultItems.OFF_FIST)),
            () -> Assertions.assertEquals(DefaultItems.characteristicsForFreeSlots(Set.of()),
                service.getEquippedCharacteristicsByPersonageIds(Set.of(personageId)).get(personageId))
        );
    }

    @ParameterizedTest
    @EnumSource(value = EquipmentCatalogVersion.class, names = {"SCALING_V4", "SCALING_V5"})
    void activeReleaseFeedsTheSameDefaultsToItemsAndVisibleCharacteristics(EquipmentCatalogVersion version) {
        final var personageId = PersonageId.from(1);
        final var dao = Mockito.mock(ItemDao.class);
        Mockito.when(dao.getEquippedByPersonageIds(Set.of(personageId))).thenReturn(Map.of());
        final var lifecycle = activeLifecycle(version);
        final var service = service(dao, lifecycle, version);
        final var items = service.getEquippedItemsByPersonageIds(Set.of(personageId)).get(personageId);
        final var characteristics = service.getEquippedCharacteristicsByPersonageIds(Set.of(personageId)).get(personageId);

        Assertions.assertAll(
            () -> Assertions.assertEquals(7, items.size()),
            () -> Assertions.assertEquals(120, forSlot(items, PersonageSlot.MAIN_HAND).itemAttacks().getFirst().attack()),
            () -> Assertions.assertEquals(60, forSlot(items, PersonageSlot.OFF_HAND).itemAttacks().getFirst().attack()),
            () -> Assertions.assertEquals(characteristics(items), characteristics),
            () -> Assertions.assertNotEquals(DefaultItems.characteristicsForFreeSlots(Set.of()), characteristics),
            () -> Assertions.assertEquals(items, service.itemsWithDefaults(List.of()))
        );
    }

    @ParameterizedTest
    @EnumSource(value = EquipmentCatalogVersion.class, names = {"SCALING_V4", "SCALING_V5"})
    void batchUsesOneActivationSnapshotAndDoesNotFillEitherHandUnderATwoHandedWeapon(
        EquipmentCatalogVersion version
    ) {
        final var first = PersonageId.from(1);
        final var second = PersonageId.from(2);
        final var personageIds = Set.of(first, second);
        final var longbow = EquipmentCatalogLoader.loadValidated(version).itemObjects()
            .stream().filter(item -> item.code().equals("longbow")).findFirst().orElseThrow();
        final var equipped = new PersonageItem(
            77, 12, longbow, Optional.empty(), Optional.empty(), ItemRarity.COMMON,
            Optional.of(second), true, 10, 4
        );
        final var dao = Mockito.mock(ItemDao.class);
        Mockito.when(dao.getEquippedByPersonageIds(personageIds)).thenReturn(Map.of(second, List.of(equipped)));
        final var lifecycle = activeLifecycle(version);
        final var service = service(dao, lifecycle, version);
        final var batch = service.getEquippedItemsByPersonageIds(personageIds);

        Assertions.assertAll(
            () -> Assertions.assertEquals(7, batch.get(first).size()),
            () -> Assertions.assertEquals(6, batch.get(second).size()),
            () -> Assertions.assertTrue(batch.get(second).contains(equipped.toItem())),
            () -> Assertions.assertEquals(1, batch.get(second).stream()
                .filter(item -> item.object().slots().contains(PersonageSlot.MAIN_HAND)).count()),
            () -> Assertions.assertEquals(1, batch.get(second).stream()
                .filter(item -> item.object().slots().contains(PersonageSlot.OFF_HAND)).count()),
            () -> Assertions.assertTrue(batch.get(second).stream()
                .noneMatch(item -> item.object().code().equals("main-fists") || item.object().code().equals("off-fists")))
        );
        Mockito.verify(lifecycle).getActiveVersions();
        Mockito.verifyNoMoreInteractions(lifecycle);
        Mockito.clearInvocations(lifecycle);
        final var characteristicBatch = service.getEquippedCharacteristicsByPersonageIds(personageIds);
        Assertions.assertEquals(characteristics(batch.get(first)), characteristicBatch.get(first));
        Assertions.assertEquals(characteristics(batch.get(second)), characteristicBatch.get(second));
        Mockito.verify(lifecycle).getActiveVersions();
        Mockito.verifyNoMoreInteractions(lifecycle);
    }

    @ParameterizedTest
    @EnumSource(value = EquipmentCatalogVersion.class, names = {"SCALING_V4", "SCALING_V5"})
    void emptyBatchDoesNotReadTheLifecycleOrInventPersonages(EquipmentCatalogVersion version) {
        final var dao = Mockito.mock(ItemDao.class);
        final var lifecycle = activeLifecycle(version);
        final var service = service(dao, lifecycle, version);

        Assertions.assertEquals(Map.of(), service.getEquippedItemsByPersonageIds(Set.of()));
        Assertions.assertEquals(Map.of(), service.getEquippedCharacteristicsByPersonageIds(Set.of()));
        Mockito.verifyNoInteractions(dao, lifecycle);
    }

    @ParameterizedTest
    @EnumSource(value = EquipmentCatalogVersion.class, names = {"SCALING_V4", "SCALING_V5"})
    void explicitlySuppliedSnapshotDoesNotReadTheLifecycleAgain(EquipmentCatalogVersion version) {
        final var dao = Mockito.mock(ItemDao.class);
        final var lifecycle = activeLifecycle(version);
        final var service = service(dao, lifecycle, version);
        final var snapshot = DefaultItemProvider.legacy().defaultsBySlot();
        final var first = service.itemsWithDefaults(List.of(), snapshot);
        final var second = service.itemsWithDefaults(List.of(), snapshot);

        Assertions.assertEquals(first, second);
        Assertions.assertTrue(first.contains(DefaultItems.MAIN_FIST));
        Mockito.verifyNoInteractions(dao, lifecycle);
    }

    private static ItemService service(
        ItemDao dao,
        ItemCatalogLifecycleDao lifecycle,
        EquipmentCatalogVersion version
    ) {
        final var config = new EquipmentReleaseConfig();
        config.setCatalogVersion(version);
        return new ItemService(null, null, dao, null, null, new DefaultItemProvider(config, lifecycle));
    }

    private static ItemCatalogLifecycleDao activeLifecycle(EquipmentCatalogVersion version) {
        final var lifecycle = Mockito.mock(ItemCatalogLifecycleDao.class);
        Mockito.when(lifecycle.getActiveVersions()).thenReturn(List.of(version));
        return lifecycle;
    }

    private static Item forSlot(List<Item> items, PersonageSlot slot) {
        return items.stream().filter(item -> item.object().slots().contains(slot)).findFirst().orElseThrow();
    }

    private static Characteristics characteristics(List<Item> items) {
        return items.stream().map(Item::visibleCharacteristics)
            .collect(Characteristics::empty, Characteristics::add, Characteristics::add);
    }
}
