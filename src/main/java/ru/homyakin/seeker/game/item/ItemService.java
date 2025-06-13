package ru.homyakin.seeker.game.item;

import io.vavr.control.Either;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.item.characteristics.ItemCharacteristicService;
import ru.homyakin.seeker.game.item.database.ItemDao;
import ru.homyakin.seeker.game.item.database.ItemObjectDao;
import ru.homyakin.seeker.game.item.errors.GenerateItemError;
import ru.homyakin.seeker.game.item.errors.PutOnItemError;
import ru.homyakin.seeker.game.item.errors.TakeOffItemError;
import ru.homyakin.seeker.game.item.models.GenerateItemParams;
import ru.homyakin.seeker.game.item.modifier.ItemModifierService;
import ru.homyakin.seeker.game.item.modifier.models.AlreadyMaxModifiers;
import ru.homyakin.seeker.game.item.modifier.models.GenerateModifier;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.modifier.models.NotBrokenItem;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.infrastructure.init.saving_models.item.ItemObjects;

@Service
public class ItemService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ItemObjectDao itemObjectDao;
    private final ItemModifierService itemModifierService;
    private final ItemDao itemDao;
    private final ItemCharacteristicService characteristicService;

    public ItemService(
        ItemObjectDao itemObjectDao,
        ItemModifierService itemModifierService,
        ItemDao itemDao,
        ItemCharacteristicService characteristicService
    ) {
        this.itemObjectDao = itemObjectDao;
        this.itemModifierService = itemModifierService;
        this.itemDao = itemDao;
        this.characteristicService = characteristicService;
    }

    public Optional<Item> getById(long id) {
        return itemDao.getById(id);
    }

    public void saveObjects(ItemObjects objects) {
        objects.object().forEach(itemObjectDao::saveObject);
    }

    public Either<GenerateItemError, Item> generateItemForPersonage(Personage personage, GenerateItemParams params) {
        final var object = itemObjectDao.getRandomObject(params.rarity(), params.slot());
        final var modifiers = itemModifierService.generate(params.rarity(), params.modifierCount());
        final var tempItem = new Item(
            0L,
            object.toItemObject(),
            params.rarity(),
            modifiers.stream().map(GenerateModifier::toModifier).toList(),
            Optional.of(personage.id()),
            false,
            false,
            characteristicService.createCharacteristics(params.rarity(), object, modifiers)
        );

        if (!personage.hasSpaceInBag(getPersonageItems(personage.id()))) {
            logger.info("Personage '{}' has no space in bag", personage.id().value());
            final var tempItemWithoutPersonageId = new Item(
                tempItem.id(),
                tempItem.object(),
                tempItem.rarity(),
                tempItem.modifiers(),
                Optional.empty(),
                tempItem.isEquipped(),
                tempItem.isBroken(),
                tempItem.characteristics()
            );
            final var id = itemDao.saveItem(tempItemWithoutPersonageId);
            return Either.left(new GenerateItemError.NotEnoughSpace(getById(id).orElseThrow()));
        }

        final var id = itemDao.saveItem(tempItem);
        return Either.right(getById(id).orElseThrow());
    }

    public Either<AlreadyMaxModifiers, Item> addModifier(Item item) {
        final var addModifierResult = itemModifierService.addModifier(item.rarity(), item.modifiers());
        if (addModifierResult.isLeft()) {
            return Either.left(addModifierResult.getLeft());
        }
        final var newModifiers = addModifierResult.get();
        final var newCharacteristics = characteristicService.createCharacteristics(
            item.rarity(),
            itemObjectDao.getById(item.object().id()),
            newModifiers
        );
        itemDao.updateItem(
            item.id(),
            newCharacteristics,
            newModifiers.stream().map(GenerateModifier::toModifier).toList()
        );
        return Either.right(getById(item.id()).orElseThrow());
    }

    public Either<NotBrokenItem, Item> repair(Item item) {
        if (!item.isBroken()) {
            return Either.left(NotBrokenItem.INSTANCE);
        }
        final var newCharacteristics = characteristicService.createCharacteristics(
            item.rarity(),
            itemObjectDao.getById(item.object().id()),
            itemModifierService.mapModifiers(item.modifiers())
        );
        itemDao.updateItem(
            item.id(),
            newCharacteristics,
            false
        );
        return Either.right(getById(item.id()).orElseThrow());
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
            .peek(_ -> itemDao.invertEquip(itemId))
            .map(_ -> itemDao.getById(itemId).orElseThrow());
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
            .peek(_ -> itemDao.invertEquip(itemId))
            .map(_ -> itemDao.getById(itemId).orElseThrow());
    }

    public Optional<Item> getPersonageItem(PersonageId personageId, long itemId) {
        return itemDao.getById(itemId)
            .filter(item -> item.personageId().map(it -> it.equals(personageId)).orElse(false));
    }

    /**
     * @return Optional.empty() если не найден
     */
    public Optional<Item> removeItem(PersonageId personageId, long itemId) {
        return getPersonageItem(personageId, itemId)
            .map(_ -> {
                itemDao.deletePersonageAndMakeEquipFalse(itemId);
                return itemDao.getById(itemId).orElseThrow();
            });
    }
}
