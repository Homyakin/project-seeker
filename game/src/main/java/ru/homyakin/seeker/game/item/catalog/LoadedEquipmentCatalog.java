package ru.homyakin.seeker.game.item.catalog;

import java.util.List;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.Modifier;

/**
 * In-memory catalog data. It has no database identifiers and cannot be persisted by this type.
 */
public record LoadedEquipmentCatalog(
    EquipmentCatalogRelease release,
    List<ItemObject> itemObjects,
    List<Modifier> modifiers,
    List<ItemObject> defaultItems
) {
    public LoadedEquipmentCatalog {
        if (release == null) {
            throw new IllegalArgumentException("Catalog release must be specified");
        }
        itemObjects = List.copyOf(itemObjects);
        modifiers = List.copyOf(modifiers);
        defaultItems = List.copyOf(defaultItems);
    }
}
