package ru.homyakin.seeker.game.item;

import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.item.catalog.ItemModifiersToml;
import ru.homyakin.seeker.game.item.catalog.ItemObjectsToml;
import ru.homyakin.seeker.game.item.database.ItemModifierDao;
import ru.homyakin.seeker.game.item.database.ItemObjectDao;

@Service
public class ItemCatalogService {
    private final ItemObjectDao itemObjectDao;
    private final ItemModifierDao itemModifierDao;

    public ItemCatalogService(ItemObjectDao itemObjectDao, ItemModifierDao itemModifierDao) {
        this.itemObjectDao = itemObjectDao;
        this.itemModifierDao = itemModifierDao;
    }

    public void saveObjects(ItemObjectsToml objects) {
        objects.item().forEach(itemObjectDao::save);
    }

    public void saveModifiers(ItemModifiersToml modifiers) {
        modifiers.modifier().forEach(itemModifierDao::save);
    }
}
