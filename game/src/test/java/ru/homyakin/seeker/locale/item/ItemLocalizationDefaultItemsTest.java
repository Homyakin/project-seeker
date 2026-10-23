package ru.homyakin.seeker.locale.item;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;
import ru.homyakin.seeker.game.battle.Position;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;
import ru.homyakin.seeker.game.battle.targeting.TargetingTactic;
import ru.homyakin.seeker.game.item.DefaultItemProvider;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogVersion;
import ru.homyakin.seeker.game.item.catalog.EquipmentReleaseConfig;
import ru.homyakin.seeker.game.item.database.ItemCatalogLifecycleDao;
import ru.homyakin.seeker.game.item.loadout.entity.EquipmentLoadout;
import ru.homyakin.seeker.game.item.models.Inventory;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.ItemObjectLocale;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.LocalizationInitializer;

class ItemLocalizationDefaultItemsTest {
    private static final Inventory EMPTY_INVENTORY = new Inventory(List.of());
    private static final EquipmentLoadout EMPTY_LOADOUT = new EquipmentLoadout(
        1L, PersonageId.from(1L), "Пустой комплект", List.of(), Position.FRONT, TargetingTactic.THREAT, java.util.Set.of()
    );

    @BeforeAll
    static void initLocalization() {
        LocalizationInitializer.initLocale();
    }

    @Test
    void allFullAndCompactViewsUseTheExplicitSnapshotWithoutFallingBackToStaticDefaults() {
        final var defaults = new EnumMap<>(DefaultItemProvider.legacy().defaultsBySlot());
        final var original = defaults.get(PersonageSlot.MAIN_HAND);
        final var object = original.object();
        defaults.put(PersonageSlot.MAIN_HAND, new Item(
            new ItemObject(
                object.code(), object.slots(), object.attacks(), object.defense(), object.health(), object.critChance(),
                object.dodgeChance(), object.critMultiplier(), object.speed(), object.baseThreat(), object.impact(),
                object.progressionVersion(), Map.of(Language.RU, new ItemObjectLocale(
                    "Контрольный кулак", object.locales().get(Language.RU).form()
                ))
            ),
            original.modifier(), original.rarity(), original.enhanceLevel()
        ));
        final var snapshot = Map.copyOf(defaults);
        final var version = SkillFormulaVersion.SCALING_SKILLS_V2;
        final var views = List.of(
            ItemLocalization.equipment(Language.RU, EMPTY_INVENTORY, version, snapshot),
            ItemLocalization.compactInventory(Language.RU, EMPTY_INVENTORY, 0, snapshot),
            ItemLocalization.loadoutDetail(Language.RU, EMPTY_LOADOUT, EMPTY_INVENTORY, false, version, snapshot),
            ItemLocalization.loadoutDetail(Language.RU, EMPTY_LOADOUT, EMPTY_INVENTORY, true, version, snapshot),
            ItemLocalization.initCreateLoadout(Language.RU, EMPTY_INVENTORY, false, version, snapshot),
            ItemLocalization.initCreateLoadout(Language.RU, EMPTY_INVENTORY, true, version, snapshot)
        );

        for (final var view : views) {
            Assertions.assertTrue(view.contains("Контрольный кулак"), view);
            Assertions.assertFalse(view.contains("${"), view);
        }
    }

    @ParameterizedTest
    @EnumSource(value = EquipmentCatalogVersion.class, names = {"SCALING_V4", "SCALING_V5"})
    void activeReleaseSnapshotMatchesItsRealItemCardsAndStagingKeepsLegacyCards(EquipmentCatalogVersion catalogVersion) {
        final var config = new EquipmentReleaseConfig();
        config.setCatalogVersion(catalogVersion);
        config.setActivateOnStartup(true);
        final var dao = Mockito.mock(ItemCatalogLifecycleDao.class);
        final var provider = new DefaultItemProvider(config, dao);
        final var stagedSnapshot = provider.defaultsBySlot();
        final var version = SkillFormulaVersion.SCALING_SKILLS_V2;
        final var stagedView = ItemLocalization.equipment(Language.RU, EMPTY_INVENTORY, version, stagedSnapshot);
        final var legacyView = ItemLocalization.equipment(Language.RU, EMPTY_INVENTORY, version);
        Assertions.assertEquals(legacyView, stagedView);
        Mockito.when(dao.getActiveVersions()).thenReturn(List.of(catalogVersion));

        final var activeSnapshot = provider.defaultsBySlot();
        final var activeView = ItemLocalization.equipment(Language.RU, EMPTY_INVENTORY, version, activeSnapshot);

        Assertions.assertNotEquals(stagedView, activeView);
        for (final var item : activeSnapshot.values()) {
            Assertions.assertTrue(activeView.contains(ItemLocalization.fullItem(Language.RU, item, version).trim()));
        }
        Assertions.assertEquals(150, stagedSnapshot.get(PersonageSlot.MAIN_HAND).itemAttacks().getFirst().attack());
        Assertions.assertEquals(120, activeSnapshot.get(PersonageSlot.MAIN_HAND).itemAttacks().getFirst().attack());
        // Existing overloads remain explicitly legacy even when callers ask for V2 skill descriptions.
        Assertions.assertEquals(legacyView, ItemLocalization.equipment(Language.RU, EMPTY_INVENTORY, version));
    }
}
