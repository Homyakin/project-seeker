package ru.homyakin.seeker.game.item.catalog;

import java.util.ArrayList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;

class EquipmentCatalogValidatorTest {
    @Test
    void duplicateItemCodeIsRejected() {
        final var catalog = EquipmentCatalogLoader.load(EquipmentCatalogVersion.SCALING_V1);
        final var duplicatedObjects = new ArrayList<>(catalog.itemObjects());
        duplicatedObjects.set(duplicatedObjects.size() - 1, duplicatedObjects.getFirst());
        final var invalidCatalog = new LoadedEquipmentCatalog(
            catalog.release(),
            duplicatedObjects,
            catalog.modifiers(),
            catalog.defaultItems()
        );

        final var exception = Assertions.assertThrows(
            IllegalStateException.class,
            () -> EquipmentCatalogValidator.validate(invalidCatalog)
        );

        Assertions.assertTrue(exception.getMessage().contains("Duplicate item object code"));
    }

    @Test
    void scalingV1CannotSilentlyUseLegacySkillFormulas() {
        final var catalog = EquipmentCatalogLoader.load(EquipmentCatalogVersion.SCALING_V1);
        final var release = catalog.release();
        final var invalidRelease = new EquipmentCatalogRelease(
            release.catalogVersion(),
            release.progressionVersion(),
            SkillFormulaVersion.LEGACY_SKILLS_V1,
            release.itemObjectsPath(),
            release.itemModifiersPath(),
            release.defaultItemsPath()
        );
        final var invalidCatalog = new LoadedEquipmentCatalog(
            invalidRelease,
            catalog.itemObjects(),
            catalog.modifiers(),
            catalog.defaultItems()
        );

        final var exception = Assertions.assertThrows(
            IllegalStateException.class,
            () -> EquipmentCatalogValidator.validate(invalidCatalog)
        );

        Assertions.assertTrue(exception.getMessage().contains("scaling skill formulas V1"));
    }

    @Test
    void scalingV2CannotSilentlyUseFirstSkillFormulas() {
        final var catalog = EquipmentCatalogLoader.load(EquipmentCatalogVersion.SCALING_V2);
        final var release = catalog.release();
        final var invalidRelease = new EquipmentCatalogRelease(
            release.catalogVersion(),
            release.progressionVersion(),
            SkillFormulaVersion.SCALING_SKILLS_V1,
            release.itemObjectsPath(),
            release.itemModifiersPath(),
            release.defaultItemsPath()
        );
        final var invalidCatalog = new LoadedEquipmentCatalog(
            invalidRelease,
            catalog.itemObjects(),
            catalog.modifiers(),
            catalog.defaultItems()
        );

        final var exception = Assertions.assertThrows(
            IllegalStateException.class,
            () -> EquipmentCatalogValidator.validate(invalidCatalog)
        );

        Assertions.assertTrue(exception.getMessage().contains("scaling skill formulas V2"));
    }
}
