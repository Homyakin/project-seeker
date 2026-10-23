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
    private static final String SCALING_V2_FOLDER = "game-data/catalog/scaling_v2/";
    private static final EquipmentCatalogRelease SCALING_V2 = new EquipmentCatalogRelease(
        EquipmentCatalogVersion.SCALING_V2,
        ItemProgressionVersion.V1,
        SkillFormulaVersion.SCALING_SKILLS_V2,
        SCALING_V2_FOLDER + "item_objects.toml",
        SCALING_V2_FOLDER + "item_modifiers.toml",
        SCALING_V2_FOLDER + "default_items.toml"
    );
    private static final String SCALING_V3_FOLDER = "game-data/catalog/scaling_v3/";
    private static final EquipmentCatalogRelease SCALING_V3 = new EquipmentCatalogRelease(
        EquipmentCatalogVersion.SCALING_V3,
        ItemProgressionVersion.V1,
        SkillFormulaVersion.SCALING_SKILLS_V2,
        SCALING_V3_FOLDER + "item_objects.toml",
        SCALING_V3_FOLDER + "item_modifiers.toml",
        SCALING_V3_FOLDER + "default_items.toml"
    );
    private static final String SCALING_V4_FOLDER = "game-data/catalog/scaling_v4/";
    private static final EquipmentCatalogRelease SCALING_V4 = new EquipmentCatalogRelease(
        EquipmentCatalogVersion.SCALING_V4,
        ItemProgressionVersion.V1,
        SkillFormulaVersion.SCALING_SKILLS_V2,
        SCALING_V4_FOLDER + "item_objects.toml",
        SCALING_V4_FOLDER + "item_modifiers.toml",
        SCALING_V4_FOLDER + "default_items.toml"
    );
    private static final String SCALING_V5_FOLDER = "game-data/catalog/scaling_v5/";
    private static final EquipmentCatalogRelease SCALING_V5 = new EquipmentCatalogRelease(
        EquipmentCatalogVersion.SCALING_V5,
        ItemProgressionVersion.V1,
        SkillFormulaVersion.SCALING_SKILLS_V2,
        SCALING_V5_FOLDER + "item_objects.toml",
        SCALING_V5_FOLDER + "item_modifiers.toml",
        SCALING_V5_FOLDER + "default_items.toml"
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
            case SCALING_V2 -> SCALING_V2;
            case SCALING_V3 -> SCALING_V3;
            case SCALING_V4 -> SCALING_V4;
            case SCALING_V5 -> SCALING_V5;
        };
    }

    private static void requirePath(String path, String resourceName) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Path for " + resourceName + " must be specified");
        }
    }
}
