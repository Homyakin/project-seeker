package ru.homyakin.seeker.game.item;

import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.item.database.ItemModifierDao;
import ru.homyakin.seeker.game.item.database.ItemObjectDao;
import ru.homyakin.seeker.infrastructure.init.saving_models.ItemModifiers;
import ru.homyakin.seeker.infrastructure.init.saving_models.ItemObjects;

@Service
public class ItemService {
    private final ItemObjectDao itemObjectDao;
    private final ItemModifierDao itemModifierDao;

    public ItemService(ItemObjectDao itemObjectDao, ItemModifierDao itemModifierDao) {
        this.itemObjectDao = itemObjectDao;
        this.itemModifierDao = itemModifierDao;
    }

    public void saveObjects(ItemObjects objects) {
        objects.object().forEach(itemObjectDao::saveObject);
    }

    public void saveModifiers(ItemModifiers modifiers) {
        modifiers.modifier().forEach(itemModifierDao::saveModifier);
    }
}
