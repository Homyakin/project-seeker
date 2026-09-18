package ru.homyakin.seeker.game.item;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogVersion;
import ru.homyakin.seeker.game.item.catalog.ItemModifiersToml;
import ru.homyakin.seeker.game.item.catalog.ItemObjectsToml;
import ru.homyakin.seeker.game.item.database.ItemCatalogLifecycleDao;
import ru.homyakin.seeker.game.item.database.ItemCatalogLifecycleDao.CatalogBackupState;
import ru.homyakin.seeker.game.item.database.ItemModifierDao;
import ru.homyakin.seeker.game.item.database.ItemObjectDao;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.Modifier;

class ItemCatalogServiceTest {
    private final ItemObjectDao itemObjectDao = Mockito.mock(ItemObjectDao.class);
    private final ItemModifierDao itemModifierDao = Mockito.mock(ItemModifierDao.class);
    private final ItemCatalogLifecycleDao lifecycleDao = Mockito.mock(ItemCatalogLifecycleDao.class);
    private final ItemCatalogService service = new ItemCatalogService(itemObjectDao, itemModifierDao, lifecycleDao);

    @Test
    void stageReleaseKeepsPreviousCatalogAvailableAndStagesNewRows() {
        final var previousItemCodes = previousItemCodes();
        final var previousModifierCodes = previousModifierCodes();
        Mockito.when(lifecycleDao.registerStagedIfMissing(EquipmentCatalogVersion.SCALING_V1)).thenReturn(true);
        Mockito.when(lifecycleDao.getCatalogBackupState()).thenReturn(new CatalogBackupState(40, 11, 40, 11));

        service.stageRelease(
            EquipmentCatalogVersion.SCALING_V1,
            previousItemObjects(),
            previousModifiers()
        );

        final var itemAvailability = capturedItemAvailability();
        final var modifierAvailability = capturedModifierAvailability();
        Assertions.assertEquals(62, itemAvailability.size());
        Assertions.assertEquals(40, itemAvailability.values().stream().filter(Boolean::booleanValue).count());
        Assertions.assertEquals(22, itemAvailability.values().stream().filter(value -> !value).count());
        Assertions.assertTrue(previousItemCodes.stream().allMatch(itemAvailability::get));
        Assertions.assertEquals(15, modifierAvailability.size());
        Assertions.assertEquals(11, modifierAvailability.values().stream().filter(Boolean::booleanValue).count());
        Assertions.assertEquals(4, modifierAvailability.values().stream().filter(value -> !value).count());
        Assertions.assertTrue(previousModifierCodes.stream().allMatch(modifierAvailability::get));
        Mockito.verify(itemObjectDao).setAcquisitionEnabledOnly(previousItemCodes);
        Mockito.verify(itemModifierDao).setAssignmentEnabledOnly(previousModifierCodes);
    }

    @Test
    void activateReleaseEnablesOnlyCompleteRelease() {
        Mockito.when(lifecycleDao.activateIfStaged(EquipmentCatalogVersion.SCALING_V1)).thenReturn(true);

        service.activateRelease(EquipmentCatalogVersion.SCALING_V1);

        final var itemCodes = captureEnabledItemCodes();
        final var modifierCodes = captureEnabledModifierCodes();
        Assertions.assertEquals(62, itemCodes.size());
        Assertions.assertEquals(15, modifierCodes.size());
        Assertions.assertTrue(itemCodes.containsAll(previousItemCodes()));
        Assertions.assertTrue(modifierCodes.containsAll(previousModifierCodes()));
        Mockito.verify(lifecycleDao).activateIfStaged(EquipmentCatalogVersion.SCALING_V1);
        Mockito.verifyNoMoreInteractions(itemObjectDao, itemModifierDao, lifecycleDao);
    }

    @Test
    void repeatedStagePreservesPersistedAvailability() {
        service.stageRelease(
            EquipmentCatalogVersion.SCALING_V1,
            previousItemObjects(),
            previousModifiers()
        );

        Mockito.verify(itemObjectDao, Mockito.never()).setAcquisitionEnabledOnly(Mockito.any());
        Mockito.verify(itemModifierDao, Mockito.never()).setAssignmentEnabledOnly(Mockito.any());
    }

    @Test
    void activeReleaseStaysActiveWhenApplicationRestarts() {
        Mockito.when(lifecycleDao.isActive(EquipmentCatalogVersion.SCALING_V1)).thenReturn(true);

        service.stageRelease(
            EquipmentCatalogVersion.SCALING_V1,
            previousItemObjects(),
            previousModifiers()
        );

        Assertions.assertTrue(capturedItemAvailability().values().stream().allMatch(Boolean::booleanValue));
        Assertions.assertTrue(capturedModifierAvailability().values().stream().allMatch(Boolean::booleanValue));
        Mockito.verify(itemObjectDao, Mockito.never()).setAcquisitionEnabledOnly(Mockito.any());
        Mockito.verify(itemModifierDao, Mockito.never()).setAssignmentEnabledOnly(Mockito.any());
    }

    @Test
    void repeatedActivationPreservesLaterRetirements() {
        service.activateRelease(EquipmentCatalogVersion.SCALING_V1);

        Mockito.verifyNoInteractions(itemObjectDao, itemModifierDao);
        Mockito.verify(lifecycleDao).activateIfStaged(EquipmentCatalogVersion.SCALING_V1);
    }

    @Test
    void stageReleaseRejectsUnknownPreviousCodesBeforeWriting() {
        final var unknownObject = Mockito.mock(ItemObject.class);
        Mockito.when(unknownObject.code()).thenReturn("removed-object");

        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> service.stageRelease(
                EquipmentCatalogVersion.SCALING_V1,
                List.of(unknownObject),
                previousModifiers()
            )
        );

        Mockito.verifyNoInteractions(itemObjectDao, itemModifierDao);
    }

    @Test
    void firstStageSnapshotsPreviousCatalogWhenMigrationRanOnEmptyDatabase() {
        Mockito.when(lifecycleDao.registerStagedIfMissing(EquipmentCatalogVersion.SCALING_V1)).thenReturn(true);
        Mockito.when(lifecycleDao.getCatalogBackupState()).thenReturn(new CatalogBackupState(0, 0, 0, 0));
        Mockito.when(lifecycleDao.snapshotItemObjects()).thenReturn(40);
        Mockito.when(lifecycleDao.snapshotItemModifiers()).thenReturn(11);

        service.stageRelease(
            EquipmentCatalogVersion.SCALING_V1,
            previousItemObjects(),
            previousModifiers()
        );

        final var itemCaptor = ArgumentCaptor.forClass(ItemObject.class);
        Mockito.verify(itemObjectDao, Mockito.times(102)).save(itemCaptor.capture(), Mockito.anyBoolean());
        Assertions.assertEquals(previousItemCodes(), itemCaptor.getAllValues().subList(0, 40).stream()
            .map(ItemObject::code)
            .collect(Collectors.toUnmodifiableSet()));
        final var modifierCaptor = ArgumentCaptor.forClass(Modifier.class);
        Mockito.verify(itemModifierDao, Mockito.times(26)).save(modifierCaptor.capture(), Mockito.anyBoolean());
        Assertions.assertEquals(previousModifierCodes(), modifierCaptor.getAllValues().subList(0, 11).stream()
            .map(Modifier::code)
            .collect(Collectors.toUnmodifiableSet()));
        Mockito.verify(lifecycleDao).snapshotItemObjects();
        Mockito.verify(lifecycleDao).snapshotItemModifiers();
    }

    @Test
    void firstStageRejectsPartialRollbackBaseline() {
        Mockito.when(lifecycleDao.registerStagedIfMissing(EquipmentCatalogVersion.SCALING_V1)).thenReturn(true);
        Mockito.when(lifecycleDao.getCatalogBackupState()).thenReturn(new CatalogBackupState(0, 11, 0, 11));

        Assertions.assertThrows(
            IllegalStateException.class,
            () -> service.stageRelease(
                EquipmentCatalogVersion.SCALING_V1,
                previousItemObjects(),
                previousModifiers()
            )
        );

        Mockito.verifyNoInteractions(itemObjectDao, itemModifierDao);
    }

    private Map<String, Boolean> capturedItemAvailability() {
        final var itemCaptor = ArgumentCaptor.forClass(ItemObject.class);
        final var availabilityCaptor = ArgumentCaptor.forClass(Boolean.class);
        Mockito.verify(itemObjectDao, Mockito.times(62)).save(itemCaptor.capture(), availabilityCaptor.capture());
        return zip(
            itemCaptor.getAllValues().stream().map(ItemObject::code).toList(),
            availabilityCaptor.getAllValues()
        );
    }

    private Map<String, Boolean> capturedModifierAvailability() {
        final var modifierCaptor = ArgumentCaptor.forClass(Modifier.class);
        final var availabilityCaptor = ArgumentCaptor.forClass(Boolean.class);
        Mockito.verify(itemModifierDao, Mockito.times(15))
            .save(modifierCaptor.capture(), availabilityCaptor.capture());
        return zip(
            modifierCaptor.getAllValues().stream().map(Modifier::code).toList(),
            availabilityCaptor.getAllValues()
        );
    }

    @SuppressWarnings("unchecked")
    private Set<String> captureEnabledItemCodes() {
        final var captor = ArgumentCaptor.forClass(Set.class);
        Mockito.verify(itemObjectDao).setAcquisitionEnabledOnly(captor.capture());
        return captor.getValue();
    }

    @SuppressWarnings("unchecked")
    private Set<String> captureEnabledModifierCodes() {
        final var captor = ArgumentCaptor.forClass(Set.class);
        Mockito.verify(itemModifierDao).setAssignmentEnabledOnly(captor.capture());
        return captor.getValue();
    }

    private static Map<String, Boolean> zip(java.util.List<String> codes, java.util.List<Boolean> values) {
        final var result = new HashMap<String, Boolean>();
        IntStream.range(0, codes.size()).forEach(index -> result.put(codes.get(index), values.get(index)));
        return result;
    }

    private static Set<String> previousItemCodes() {
        return previousItemObjects()
            .stream()
            .map(ItemObject::code)
            .collect(Collectors.toUnmodifiableSet());
    }

    private static Set<String> previousModifierCodes() {
        return previousModifiers()
            .stream()
            .map(Modifier::code)
            .collect(Collectors.toUnmodifiableSet());
    }

    private static List<ItemObject> previousItemObjects() {
        return load("game-data/item_objects_catalog.toml", ItemObjectsToml::load).itemObjects();
    }

    private static List<Modifier> previousModifiers() {
        return load("game-data/item_modifiers_catalog.toml", ItemModifiersToml::load).modifiers();
    }

    private static <T> T load(String path, Function<InputStream, T> loader) {
        try (final var stream = ItemCatalogServiceTest.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing test resource: " + path);
            }
            return loader.apply(stream);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Failed to close test resource: " + path, e);
        }
    }
}
