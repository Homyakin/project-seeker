package ru.homyakin.seeker.game.item;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.item.database.ItemModifierDao;
import ru.homyakin.seeker.game.item.database.ItemObjectDao;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.infrastructure.init.saving_models.item.ItemModifiers;
import ru.homyakin.seeker.infrastructure.init.saving_models.item.ItemObjects;

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

    public Item generateItemForPersonage(long personageId) {
        final var object = itemObjectDao.getRandomObject();
        final var modifier = itemModifierDao.getRandomModifier();
        return new Item(
            0L,
            object,
            List.of(modifier),
            Optional.of(personageId),
            false,
            0
        );
    }
}
