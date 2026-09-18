package ru.homyakin.seeker.game.item.catalog;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;
import ru.homyakin.seeker.game.item.models.ItemProgressionVersion;

class EquipmentCatalogLoaderTest {
    @Test
    void scalingReleaseBindsCatalogAndFormulaVersions() {
        final var release = EquipmentCatalogRelease.forVersion(EquipmentCatalogVersion.SCALING_V1);

        Assertions.assertEquals(EquipmentCatalogVersion.SCALING_V1, release.catalogVersion());
        Assertions.assertEquals(ItemProgressionVersion.V1, release.progressionVersion());
        Assertions.assertEquals(SkillFormulaVersion.SCALING_SKILLS_V1, release.skillFormulaVersion());
        Assertions.assertEquals("game-data/catalog/scaling_v1/item_objects.toml", release.itemObjectsPath());
        Assertions.assertEquals("game-data/catalog/scaling_v1/item_modifiers.toml", release.itemModifiersPath());
        Assertions.assertEquals("game-data/catalog/scaling_v1/default_items.toml", release.defaultItemsPath());
    }

    @Test
    void missingReleaseResourceFailsInsteadOfReturningAnEmptyCatalog() {
        final var release = new EquipmentCatalogRelease(
            EquipmentCatalogVersion.SCALING_V1,
            ItemProgressionVersion.V1,
            SkillFormulaVersion.SCALING_SKILLS_V1,
            "missing/item_objects.toml",
            "missing/item_modifiers.toml",
            "missing/default_items.toml"
        );

        final var exception = Assertions.assertThrows(
            IllegalStateException.class,
            () -> EquipmentCatalogLoader.load(release)
        );

        Assertions.assertTrue(exception.getMessage().contains("missing/item_objects.toml"));
    }

    @Test
    void scalingReleaseLoadsAndPassesAllCatalogInvariants() {
        final var catalog = EquipmentCatalogLoader.loadValidated(EquipmentCatalogVersion.SCALING_V1);

        Assertions.assertEquals(62, catalog.itemObjects().size());
        Assertions.assertEquals(15, catalog.modifiers().size());
        Assertions.assertEquals(7, catalog.defaultItems().size());
    }
}
