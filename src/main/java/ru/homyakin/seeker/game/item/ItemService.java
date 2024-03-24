package ru.homyakin.seeker.game.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.item.database.ItemDao;
import ru.homyakin.seeker.game.item.database.ItemModifierDao;
import ru.homyakin.seeker.game.item.database.ItemObjectDao;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.GenerateItemObject;
import ru.homyakin.seeker.game.item.models.GenerateModifier;
import ru.homyakin.seeker.game.item.models.ModifierType;
import ru.homyakin.seeker.game.personage.models.Characteristics;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.infrastructure.init.saving_models.item.ItemModifiers;
import ru.homyakin.seeker.infrastructure.init.saving_models.item.ItemObjects;
import ru.homyakin.seeker.utils.RandomUtils;
import ru.homyakin.seeker.utils.models.IntRange;

@Service
public class ItemService {
    private final ItemObjectDao itemObjectDao;
    private final ItemModifierDao itemModifierDao;
    private final ItemDao itemDao;

    public ItemService(ItemObjectDao itemObjectDao, ItemModifierDao itemModifierDao, ItemDao itemDao) {
        this.itemObjectDao = itemObjectDao;
        this.itemModifierDao = itemModifierDao;
        this.itemDao = itemDao;
    }

    public void saveObjects(ItemObjects objects) {
        objects.object().forEach(itemObjectDao::saveObject);
    }

    public void saveModifiers(ItemModifiers modifiers) {
        modifiers.modifier().forEach(itemModifierDao::saveModifier);
    }

    public Item generateItemForPersonage(PersonageId personageId) {
        // TODO проверить количество предметов у персонажа
        final var object = itemObjectDao.getRandomObject();
        final var modifiers = new ArrayList<GenerateModifier>();
        if (RandomUtils.bool()) {
            final var modifier = itemModifierDao.getRandomModifier();
            modifiers.add(modifier);
            if (RandomUtils.bool()) {
                // Может быть либо 2 префиксных, либо 1 суффикс и 1 префикс
                if (modifier.type() == ModifierType.PREFIX) {
                    modifiers.add(itemModifierDao.getRandomModifierExcludeId(modifier.id()));
                } else {
                    modifiers.add(itemModifierDao.getRandomModifierWithType(ModifierType.PREFIX));
                }
            }
        }
        final var tempItem = new Item(
            0L,
            object.toItemObject(),
            modifiers.stream().map(GenerateModifier::toModifier).toList(),
            Optional.of(personageId),
            false,
            createCharacteristics(object, modifiers)
        );

        final var id = itemDao.saveItem(tempItem);
        return itemDao.getById(id);
    }

    private Characteristics createCharacteristics(GenerateItemObject object, List<GenerateModifier> modifiers) {
        var attack = object.characteristics().attack().map(IntRange::value).orElse(0);
        attack += modifiers.stream()
            .map(modifier -> modifier.characteristics().attack().map(IntRange::value).orElse(0))
            .reduce(0, Integer::sum);

        return new Characteristics(
            /*health*/ 0,
            /*attack*/ attack,
            /*defense*/ 0,
            /*strength*/ 0,
            /*agility*/ 0,
            /*wisdom*/ 0
        );
    }
}
