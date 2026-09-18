package ru.homyakin.seeker.game.item.catalog;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.item.models.DefaultItems;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.ItemProgressionVersion;
import ru.homyakin.seeker.game.item.models.Modifier;

class ScalingV1CatalogIsolationTest {
    private static final Set<String> PRODUCTION_ITEM_CODES = Set.of(
        "sword", "rapier", "mace", "spear", "staff",
        "bow", "longbow", "crossbow", "two_handed_sword", "sledgehammer", "halberd",
        "shortsword", "club", "dagger", "dirk", "orb",
        "buckler", "shield", "tower_shield", "tome",
        "robe", "cuirass", "breastplate", "wizard_robe",
        "cloth_chausses", "leather_chausses", "greaves", "arcane_chausses",
        "cloth_boots", "boots", "sabatons", "arcane_boots",
        "hood", "leather_helm", "great_helm", "circlet",
        "cloth_gloves", "leather_gloves", "gauntlets", "arcane_gloves"
    );
    private static final Set<String> PRODUCTION_MODIFIER_CODES = Set.of(
        "counter", "thorny", "double", "furious", "swift", "sharp",
        "knocking", "healing", "precise", "retreating", "cunning"
    );

    @Test
    void productionResourcesRemainTheLegacyFortyAndEleven() {
        final var productionObjects = loadProductionObjects();
        final var productionModifiers = loadProductionModifiers();

        Assertions.assertEquals(PRODUCTION_ITEM_CODES, codes(productionObjects, ItemObject::code));
        Assertions.assertEquals(40, productionObjects.size());
        Assertions.assertTrue(productionObjects.stream()
            .allMatch(it -> it.progressionVersion() == ItemProgressionVersion.LEGACY));
        Assertions.assertTrue(productionObjects.stream().allMatch(it -> it.impact() == 0));

        Assertions.assertEquals(PRODUCTION_MODIFIER_CODES, codes(productionModifiers, Modifier::code));
        Assertions.assertEquals(11, productionModifiers.size());
    }

    @Test
    void inactiveReleaseUsesDifferentResourcePathsAndPreservesExistingIdentityFields() {
        final var release = EquipmentCatalogRelease.forVersion(EquipmentCatalogVersion.SCALING_V1);
        Assertions.assertAll(
            () -> Assertions.assertEquals(
                "game-data/catalog/scaling_v1/item_objects.toml",
                release.itemObjectsPath()
            ),
            () -> Assertions.assertEquals(
                "game-data/catalog/scaling_v1/item_modifiers.toml",
                release.itemModifiersPath()
            ),
            () -> Assertions.assertEquals(
                "game-data/catalog/scaling_v1/default_items.toml",
                release.defaultItemsPath()
            ),
            () -> Assertions.assertNotEquals("game-data/item_objects_catalog.toml", release.itemObjectsPath()),
            () -> Assertions.assertNotEquals("game-data/item_modifiers_catalog.toml", release.itemModifiersPath())
        );

        final var productionByCode = loadProductionObjects().stream()
            .collect(Collectors.toMap(ItemObject::code, Function.identity()));
        final var scalingByCode = EquipmentCatalogLoader.load(EquipmentCatalogVersion.SCALING_V1).itemObjects().stream()
            .collect(Collectors.toMap(ItemObject::code, Function.identity()));
        Assertions.assertTrue(scalingByCode.keySet().containsAll(PRODUCTION_ITEM_CODES));

        for (final var code : PRODUCTION_ITEM_CODES) {
            final var production = productionByCode.get(code);
            final var scaling = scalingByCode.get(code);
            Assertions.assertEquals(production.slots(), scaling.slots(), code);
            Assertions.assertEquals(production.locales(), scaling.locales(), code);
            Assertions.assertNotEquals(combatSignature(production), combatSignature(scaling), code);
        }
    }

    @Test
    void liveDefaultItemsRemainLegacyUntilTheReleaseIsActivated() {
        final var liveDefaults = List.of(
            DefaultItems.MAIN_FIST,
            DefaultItems.OFF_FIST,
            DefaultItems.SHIRT,
            DefaultItems.PANTS,
            DefaultItems.SHOES,
            DefaultItems.HELMET,
            DefaultItems.GLOVES
        ).stream().map(Item::object).collect(Collectors.toMap(ItemObject::code, Function.identity()));
        final var inactiveDefaults = EquipmentCatalogLoader.load(EquipmentCatalogVersion.SCALING_V1).defaultItems().stream()
            .collect(Collectors.toMap(ItemObject::code, Function.identity()));

        Assertions.assertEquals(liveDefaults.keySet(), inactiveDefaults.keySet());
        Assertions.assertTrue(liveDefaults.values().stream()
            .allMatch(it -> it.progressionVersion() == ItemProgressionVersion.LEGACY));
        Assertions.assertTrue(inactiveDefaults.values().stream()
            .allMatch(it -> it.progressionVersion() == ItemProgressionVersion.V1));
        for (final var code : liveDefaults.keySet()) {
            Assertions.assertNotEquals(
                combatSignature(liveDefaults.get(code)),
                combatSignature(inactiveDefaults.get(code)),
                code
            );
        }

        Assertions.assertEquals(150, DefaultItems.MAIN_FIST.itemAttacks().getFirst().attack());
        Assertions.assertEquals(50, DefaultItems.OFF_FIST.itemAttacks().getFirst().attack());
    }

    private static String combatSignature(ItemObject object) {
        return List.of(
            object.attacks(),
            object.defense(),
            object.health(),
            object.critChance(),
            object.dodgeChance(),
            object.critMultiplier(),
            object.speed(),
            object.baseThreat(),
            object.impact()
        ).toString();
    }

    private static List<ItemObject> loadProductionObjects() {
        return loadResource("game-data/item_objects_catalog.toml", ItemObjectsToml::load).itemObjects();
    }

    private static List<Modifier> loadProductionModifiers() {
        return loadResource("game-data/item_modifiers_catalog.toml", ItemModifiersToml::load).modifiers();
    }

    private static <T> Set<String> codes(List<T> values, Function<T, String> code) {
        return values.stream().map(code).collect(Collectors.toSet());
    }

    private static <T> T loadResource(String path, Function<InputStream, T> loader) {
        final var stream = ScalingV1CatalogIsolationTest.class.getClassLoader().getResourceAsStream(path);
        Assertions.assertNotNull(stream, path);
        try (stream) {
            return loader.apply(stream);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
