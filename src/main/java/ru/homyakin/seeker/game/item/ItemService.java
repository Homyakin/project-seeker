package ru.homyakin.seeker.game.item;

import io.vavr.control.Either;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.item.database.ItemDao;
import ru.homyakin.seeker.game.item.database.ItemModifierDao;
import ru.homyakin.seeker.game.item.database.ItemObjectDao;
import ru.homyakin.seeker.game.item.models.DropItemError;
import ru.homyakin.seeker.game.item.models.GenerateItemError;
import ru.homyakin.seeker.game.item.models.GenerateItemObject;
import ru.homyakin.seeker.game.item.models.GenerateModifier;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ModifierType;
import ru.homyakin.seeker.game.item.models.PutOnItemError;
import ru.homyakin.seeker.game.item.models.TakeOffItemError;
import ru.homyakin.seeker.game.personage.models.Characteristics;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.infrastructure.init.saving_models.item.ItemModifiers;
import ru.homyakin.seeker.infrastructure.init.saving_models.item.ItemObjects;
import ru.homyakin.seeker.utils.RandomUtils;
import ru.homyakin.seeker.utils.models.DoubleRange;
import ru.homyakin.seeker.utils.models.IntRange;

@Service
public class ItemService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
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

    public Either<GenerateItemError, Item> generateItemForPersonage(Personage personage) {
        // TODO проверить количество предметов у персонажа
        final var object = itemObjectDao.getRandomObject();
        final var modifiers = new ArrayList<GenerateModifier>();
        if (RandomUtils.bool()) {
            final var modifier = itemModifierDao.getRandomModifier();
            modifiers.add(modifier);
            if (RandomUtils.bool()) {
                // Может быть либо 2 префиксных, либо 1 суффикс и 1 префикс
                if (modifier.type() == ModifierType.PREFIX) {
                    modifiers.add(itemModifierDao.getRandomModifierWithType(ModifierType.SUFFIX));
                } else {
                    modifiers.add(itemModifierDao.getRandomModifierExcludeId(modifier.id()));
                }
            }
        }
        final var tempItem = new Item(
            0L,
            object.toItemObject(),
            modifiers.stream().map(GenerateModifier::toModifier).toList(),
            Optional.of(personage.id()),
            false,
            createCharacteristics(object, modifiers)
        );

        if (!personage.hasSpaceInBag(getPersonageItems(personage.id()))) {
            return Either.left(new GenerateItemError.NotEnoughSpace(tempItem));
        }

        final var id = itemDao.saveItem(tempItem);
        return Either.right(itemDao.getById(id).orElseThrow());
    }

    public List<Item> getPersonageItems(PersonageId personageId) {
        return itemDao.getByPersonageId(personageId);
    }

    public Either<PutOnItemError, Item> putOnItem(Personage personage, long itemId) {
        final var itemResult = itemDao.getById(itemId);
        if (itemResult.isEmpty()) {
            logger.debug("Personage {} tried to equip incorrect item with id {}", personage.id(), itemId);
            return Either.left(PutOnItemError.PersonageMissingItem.INSTANCE);
        }

        return personage.canPutOnItem(
                getPersonageItems(personage.id()),
                itemResult.get()
            )
            .peek(it -> itemDao.invertEquip(itemId))
            .map(it -> itemDao.getById(itemId).orElseThrow());
    }

    public Either<TakeOffItemError, Item> takeOffItem(Personage personage, long itemId) {
        final var itemResult = itemDao.getById(itemId);
        if (itemResult.isEmpty()) {
            logger.debug("Personage {} tried to take off incorrect item with id {}", personage.id(), itemId);
            return Either.left(TakeOffItemError.PersonageMissingItem.INSTANCE);
        }

        return personage.canTakeOffItem(
                getPersonageItems(personage.id()),
                itemResult.get()
            )
            .peek(it -> itemDao.invertEquip(itemId))
            .map(it -> itemDao.getById(itemId).orElseThrow());
    }

    public Either<DropItemError, Item> canDropItem(Personage personage, long itemId) {
        final var itemResult = itemDao.getById(itemId);
        if (itemResult.isEmpty()) {
            logger.debug("Personage {} tried to take off incorrect item with id {}", personage.id(), itemId);
            return Either.left(DropItemError.PersonageMissingItem.INSTANCE);
        }
        final var item = itemResult.get();
        if (!item.personageId().map(it -> it.equals(personage.id())).orElse(false)) {
            return Either.left(DropItemError.PersonageMissingItem.INSTANCE);
        }

        return Either.right(item);
    }

    public Either<DropItemError, Item> dropItem(Personage personage, long itemId) {
        return canDropItem(personage, itemId)
            .peek(it -> itemDao.deletePersonageAndMakeEquipFalse(itemId))
            .map(it -> itemDao.getById(itemId).orElseThrow());
    }

    private Characteristics createCharacteristics(GenerateItemObject object, List<GenerateModifier> modifiers) {
        int attack = object.characteristics().attack().map(IntRange::value).orElse(0);
        double multiplier = object.characteristics().multiplier().map(DoubleRange::value).orElse(1.0);
        for (final var modifier: modifiers) {
            attack += modifier.characteristics().attack().map(IntRange::value).orElse(0);
            multiplier *= modifier.characteristics().multiplier().map(DoubleRange::value).orElse(1.0);
        }

        return new Characteristics(
            /*health*/ 0,
            /*attack*/ (int) Math.round(attack * multiplier),
            /*defense*/ 0,
            /*strength*/ 0,
            /*agility*/ 0,
            /*wisdom*/ 0
        );
    }
}
