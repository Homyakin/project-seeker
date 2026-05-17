package ru.homyakin.seeker.game.item;

import io.vavr.control.Either;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.item.characteristics.LegacyItemCharacteristicService;
import ru.homyakin.seeker.game.item.database.LegacyItemDao;
import ru.homyakin.seeker.game.item.database.LegacyItemObjectDao;
import ru.homyakin.seeker.game.item.errors.LegacyGenerateItemError;
import ru.homyakin.seeker.game.item.errors.LegacyPutOnItemError;
import ru.homyakin.seeker.game.item.errors.LegacyTakeOffItemError;
import ru.homyakin.seeker.game.item.models.LegacyGenerateItemParams;
import ru.homyakin.seeker.game.item.modifier.LegacyItemModifierService;
import ru.homyakin.seeker.game.item.modifier.models.LegacyAlreadyMaxModifiers;
import ru.homyakin.seeker.game.item.modifier.models.LegacyGenerateModifier;
import ru.homyakin.seeker.game.item.models.LegacyItem;
import ru.homyakin.seeker.game.item.modifier.models.LegacyNotBrokenItem;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.infrastructure.init.saving_models.item.LegacyItemObjects;

@Service
public class LegacyItemService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final LegacyItemObjectDao itemObjectDao;
    private final LegacyItemModifierService itemModifierService;
    private final LegacyItemDao itemDao;
    private final LegacyItemCharacteristicService characteristicService;

    public LegacyItemService(
        LegacyItemObjectDao itemObjectDao,
        LegacyItemModifierService itemModifierService,
        LegacyItemDao itemDao,
        LegacyItemCharacteristicService characteristicService
    ) {
        this.itemObjectDao = itemObjectDao;
        this.itemModifierService = itemModifierService;
        this.itemDao = itemDao;
        this.characteristicService = characteristicService;
    }

    public Optional<LegacyItem> getById(long id) {
        return itemDao.getById(id);
    }

    public void saveObjects(LegacyItemObjects objects) {
        objects.object().forEach(itemObjectDao::saveObject);
    }

    public Either<LegacyGenerateItemError, LegacyItem> generateItemForPersonage(Personage personage, LegacyGenerateItemParams params) {
        final var object = itemObjectDao.getRandomObject(params.rarity(), params.slot());
        final var modifiers = itemModifierService.generate(params.rarity(), params.modifierCount());
        final var tempItem = new LegacyItem(
            0L,
            object.toItemObject(),
            params.rarity(),
            modifiers.stream().map(LegacyGenerateModifier::toModifier).toList(),
            Optional.of(personage.id()),
            false,
            false,
            characteristicService.createCharacteristics(params.rarity(), object, modifiers)
        );

        if (!personage.hasSpaceInBag(getPersonageItems(personage.id()))) {
            logger.info("Personage '{}' has no space in bag", personage.id().value());
            final var tempItemWithoutPersonageId = new LegacyItem(
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
            return Either.left(new LegacyGenerateItemError.NotEnoughSpace(getById(id).orElseThrow()));
        }

        final var id = itemDao.saveItem(tempItem);
        return Either.right(getById(id).orElseThrow());
    }

    public Either<LegacyAlreadyMaxModifiers, LegacyItem> addModifier(LegacyItem item) {
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
            newModifiers.stream().map(LegacyGenerateModifier::toModifier).toList()
        );
        return Either.right(getById(item.id()).orElseThrow());
    }

    public Either<LegacyNotBrokenItem, LegacyItem> repair(LegacyItem item) {
        if (!item.isBroken()) {
            return Either.left(LegacyNotBrokenItem.INSTANCE);
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

    public List<LegacyItem> getPersonageItems(PersonageId personageId) {
        return itemDao.getByPersonageId(personageId);
    }

    public Either<LegacyPutOnItemError, LegacyItem> putOnItem(Personage personage, long itemId) {
        final var itemResult = itemDao.getById(itemId);
        if (itemResult.isEmpty()) {
            logger.debug("Personage {} tried to equip incorrect item with id {}", personage.id(), itemId);
            return Either.left(LegacyPutOnItemError.PersonageMissingItem.INSTANCE);
        }

        return personage.canPutOnItem(
                getPersonageItems(personage.id()),
                itemResult.get()
            )
            .peek(_ -> itemDao.invertEquip(itemId))
            .map(_ -> itemDao.getById(itemId).orElseThrow());
    }

    public Either<LegacyTakeOffItemError, LegacyItem> takeOffItem(Personage personage, long itemId) {
        final var itemResult = itemDao.getById(itemId);
        if (itemResult.isEmpty()) {
            logger.debug("Personage {} tried to take off incorrect item with id {}", personage.id(), itemId);
            return Either.left(LegacyTakeOffItemError.PersonageMissingItem.INSTANCE);
        }

        return personage.canTakeOffItem(
                getPersonageItems(personage.id()),
                itemResult.get()
            )
            .peek(_ -> itemDao.invertEquip(itemId))
            .map(_ -> itemDao.getById(itemId).orElseThrow());
    }

    public Optional<LegacyItem> getPersonageItem(PersonageId personageId, long itemId) {
        return itemDao.getById(itemId)
            .filter(item -> item.personageId().map(it -> it.equals(personageId)).orElse(false));
    }

    /**
     * @return Optional.empty() если не найден
     */
    public Optional<LegacyItem> removeItem(PersonageId personageId, long itemId) {
        return getPersonageItem(personageId, itemId)
            .map(_ -> {
                itemDao.deletePersonageAndMakeEquipFalse(itemId);
                return itemDao.getById(itemId).orElseThrow();
            });
    }
}
