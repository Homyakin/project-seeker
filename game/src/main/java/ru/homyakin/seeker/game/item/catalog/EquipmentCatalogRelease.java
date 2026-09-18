package ru.homyakin.seeker.game.item.catalog;

import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;
import ru.homyakin.seeker.game.item.models.ItemProgressionVersion;

/**
 * Immutable description of one catalog release and the formulas it was designed for.
 */
public record EquipmentCatalogRelease(
    EquipmentCatalogVersion catalogVersion,
    ItemProgressionVersion progressionVersion,
    SkillFormulaVersion skillFormulaVersion,
    String itemObjectsPath,
    String itemModifiersPath,
    String defaultItemsPath
) {
    private static final String SCALING_V1_FOLDER = "game-data/catalog/scaling_v1/";
    private static final EquipmentCatalogRelease SCALING_V1 = new EquipmentCatalogRelease(
        EquipmentCatalogVersion.SCALING_V1,
        ItemProgressionVersion.V1,
        SkillFormulaVersion.SCALING_SKILLS_V1,
        SCALING_V1_FOLDER + "item_objects.toml",
        SCALING_V1_FOLDER + "item_modifiers.toml",
        SCALING_V1_FOLDER + "default_items.toml"
    );

    public EquipmentCatalogRelease {
        if (catalogVersion == null) {
            throw new IllegalArgumentException("Catalog version must be specified");
        }
        if (progressionVersion == null) {
            throw new IllegalArgumentException("Progression version must be specified");
        }
        if (skillFormulaVersion == null) {
            throw new IllegalArgumentException("Skill formula version must be specified");
        }
        requirePath(itemObjectsPath, "item objects");
        requirePath(itemModifiersPath, "item modifiers");
        requirePath(defaultItemsPath, "default items");
    }

    public static EquipmentCatalogRelease forVersion(EquipmentCatalogVersion version) {
        if (version == null) {
            throw new IllegalArgumentException("Catalog version must be specified");
        }
        return switch (version) {
            case SCALING_V1 -> SCALING_V1;
        };
    }

    private static void requirePath(String path, String resourceName) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Path for " + resourceName + " must be specified");
        }
    }
}
