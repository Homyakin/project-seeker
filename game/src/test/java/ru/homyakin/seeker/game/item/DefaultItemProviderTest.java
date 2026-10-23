package ru.homyakin.seeker.game.item;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogLoader;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogVersion;
import ru.homyakin.seeker.game.item.catalog.EquipmentReleaseConfig;
import ru.homyakin.seeker.game.item.database.ItemCatalogLifecycleDao;
import ru.homyakin.seeker.game.item.models.DefaultItems;
import ru.homyakin.seeker.game.item.models.ItemProgressionVersion;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;

class DefaultItemProviderTest {
    @Test
    void legacyProviderPreservesTheHistoricalStaticDefaults() {
        final var expected = java.util.Arrays.stream(PersonageSlot.values())
            .collect(Collectors.toUnmodifiableMap(
                Function.identity(), slot -> DefaultItems.defaultItemForSlot(slot, Set.of()).orElseThrow()
            ));
        Assertions.assertEquals(expected, DefaultItemProvider.legacy().defaultsBySlot());
        Assertions.assertEquals(150, DefaultItems.MAIN_FIST.itemAttacks().getFirst().attack());
        Assertions.assertEquals(50, DefaultItems.OFF_FIST.itemAttacks().getFirst().attack());
    }

    @ParameterizedTest
    @EnumSource(value = EquipmentCatalogVersion.class, names = {"SCALING_V4", "SCALING_V5"})
    void selectedButNotActiveReleaseDoesNotChangeDefaultsEvenWhenStartupActivationIsRequested(
        EquipmentCatalogVersion version
    ) {
        final var config = release(version);
        config.setActivateOnStartup(true);
        final var lifecycle = Mockito.mock(ItemCatalogLifecycleDao.class);
        Mockito.when(lifecycle.getActiveVersions()).thenReturn(List.of());
        final var provider = new DefaultItemProvider(config, lifecycle);

        Assertions.assertEquals(DefaultItemProvider.legacy().defaultsBySlot(), provider.defaultsBySlot());
        Mockito.verify(lifecycle).getActiveVersions();
        Mockito.verifyNoMoreInteractions(lifecycle);
    }

    @ParameterizedTest
    @EnumSource(value = EquipmentCatalogVersion.class, names = {"SCALING_V4", "SCALING_V5"})
    void anActiveDifferentReleaseDoesNotEnableSelectedReleaseDefaults(EquipmentCatalogVersion version) {
        final var config = release(version);
        final var lifecycle = Mockito.mock(ItemCatalogLifecycleDao.class);
        final var otherSupportedVersion = version == EquipmentCatalogVersion.SCALING_V4
            ? EquipmentCatalogVersion.SCALING_V5 : EquipmentCatalogVersion.SCALING_V4;
        for (final var activeVersion : List.of(EquipmentCatalogVersion.SCALING_V3, otherSupportedVersion)) {
            Mockito.when(lifecycle.getActiveVersions()).thenReturn(List.of(activeVersion));
            Assertions.assertEquals(
                DefaultItemProvider.legacy().defaultsBySlot(),
                new DefaultItemProvider(config, lifecycle).defaultsBySlot()
            );
        }
    }

    @Test
    void anEarlierSelectedReleaseKeepsLegacyDefaultsWithoutReadingTheLifecycle() {
        final var config = new EquipmentReleaseConfig();
        config.setCatalogVersion(EquipmentCatalogVersion.SCALING_V3);
        final var lifecycle = Mockito.mock(ItemCatalogLifecycleDao.class);

        Assertions.assertEquals(
            DefaultItemProvider.legacy().defaultsBySlot(),
            new DefaultItemProvider(config, lifecycle).defaultsBySlot()
        );
        Mockito.verifyNoInteractions(lifecycle);
    }

    @ParameterizedTest
    @EnumSource(value = EquipmentCatalogVersion.class, names = {"SCALING_V4", "SCALING_V5"})
    void activeSelectedReleaseUsesExactlyTheValidatedCatalogDefaults(EquipmentCatalogVersion version) {
        final var lifecycle = Mockito.mock(ItemCatalogLifecycleDao.class);
        Mockito.when(lifecycle.getActiveVersions()).thenReturn(List.of(version));
        final var provider = new DefaultItemProvider(release(version), lifecycle);
        final var snapshot = provider.defaultsBySlot();
        final var expectedObjects = EquipmentCatalogLoader.loadValidated(version)
            .defaultItems().stream().collect(Collectors.toUnmodifiableMap(
                object -> object.slots().iterator().next(), Function.identity()
            ));

        Assertions.assertEquals(Set.of(PersonageSlot.values()), snapshot.keySet());
        for (final var entry : snapshot.entrySet()) {
            final var item = entry.getValue();
            Assertions.assertAll(
                entry.getKey().name(),
                () -> Assertions.assertEquals(expectedObjects.get(entry.getKey()), item.object()),
                () -> Assertions.assertEquals(ItemRarity.COMMON, item.rarity()),
                () -> Assertions.assertEquals(0, item.enhanceLevel()),
                () -> Assertions.assertTrue(item.modifier().isEmpty()),
                () -> Assertions.assertEquals(ItemProgressionVersion.V1, item.object().progressionVersion())
            );
        }
        Assertions.assertEquals(120, snapshot.get(PersonageSlot.MAIN_HAND).itemAttacks().getFirst().attack());
        Assertions.assertEquals(60, snapshot.get(PersonageSlot.OFF_HAND).itemAttacks().getFirst().attack());
        Assertions.assertThrows(UnsupportedOperationException.class,
            () -> snapshot.put(PersonageSlot.BODY, DefaultItems.SHIRT));
    }

    @ParameterizedTest
    @EnumSource(value = EquipmentCatalogVersion.class, names = {"SCALING_V4", "SCALING_V5"})
    void immutableCatalogValuesAreCachedButTheActivationGateIsNotLatched(EquipmentCatalogVersion version) {
        final var lifecycle = Mockito.mock(ItemCatalogLifecycleDao.class);
        Mockito.when(lifecycle.getActiveVersions()).thenReturn(
            List.of(), List.of(version), List.of(version), List.of()
        );
        final var provider = new DefaultItemProvider(release(version), lifecycle);
        final var legacy = provider.defaultsBySlot();
        final var active = provider.defaultsBySlot();
        final var repeated = provider.defaultsBySlot();
        final var inactive = provider.defaultsBySlot();

        Assertions.assertAll(
            () -> Assertions.assertSame(active, repeated),
            () -> Assertions.assertSame(legacy, inactive),
            () -> Assertions.assertNotEquals(legacy, active),
            () -> Assertions.assertEquals(150, legacy.get(PersonageSlot.MAIN_HAND).itemAttacks().getFirst().attack()),
            () -> Assertions.assertEquals(120, active.get(PersonageSlot.MAIN_HAND).itemAttacks().getFirst().attack())
        );
        Mockito.verify(lifecycle, Mockito.times(4)).getActiveVersions();
    }

    private static EquipmentReleaseConfig release(EquipmentCatalogVersion version) {
        final var config = new EquipmentReleaseConfig();
        config.setCatalogVersion(version);
        return config;
    }
}
