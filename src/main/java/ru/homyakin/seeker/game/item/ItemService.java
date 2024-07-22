package ru.homyakin.seeker.game.item;

import io.vavr.control.Either;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.item.characteristics.ItemCharacteristicService;
import ru.homyakin.seeker.game.item.database.ItemDao;
import ru.homyakin.seeker.game.item.database.ItemModifierDao;
import ru.homyakin.seeker.game.item.database.ItemObjectDao;
import ru.homyakin.seeker.game.item.errors.DropItemError;
import ru.homyakin.seeker.game.item.errors.GenerateItemError;
import ru.homyakin.seeker.game.item.errors.PutOnItemError;
import ru.homyakin.seeker.game.item.errors.TakeOffItemError;
import ru.homyakin.seeker.game.item.models.GenerateModifier;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ModifierType;
import ru.homyakin.seeker.game.item.rarity.ItemRarity;
import ru.homyakin.seeker.game.item.rarity.ItemRarityService;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.infrastructure.init.saving_models.item.ItemModifiers;
import ru.homyakin.seeker.infrastructure.init.saving_models.item.ItemObjects;
import ru.homyakin.seeker.utils.RandomUtils;

@Service
public class ItemService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ItemObjectDao itemObjectDao;
    private final ItemModifierDao itemModifierDao;
    private final ItemDao itemDao;
    private final ItemCharacteristicService characteristicService;
    private final ItemRarityService rarityService;

    public ItemService(
        ItemObjectDao itemObjectDao,
        ItemModifierDao itemModifierDao,
        ItemDao itemDao,
        ItemCharacteristicService characteristicService,
        ItemRarityService rarityService
    ) {
        this.itemObjectDao = itemObjectDao;
        this.itemModifierDao = itemModifierDao;
        this.itemDao = itemDao;
        this.characteristicService = characteristicService;
        this.rarityService = rarityService;
    }

    public void saveObjects(ItemObjects objects) {
        objects.object().forEach(itemObjectDao::saveObject);
    }

    public void saveModifiers(ItemModifiers modifiers) {
        modifiers.modifier().forEach(itemModifierDao::saveModifier);
    }

    public Either<GenerateItemError, Item> generateItemForPersonage(Personage personage) {
        final var itemRarity = rarityService.generateItemRarity();
        return generateItemWithRarity(personage, itemRarity);
    }

    public Either<GenerateItemError, Item> generateItemWithRarity(Personage personage, ItemRarity rarity) {
        final var object = itemObjectDao.getRandomObject(rarity);
        final var modifiers = new ArrayList<GenerateModifier>();
        if (RandomUtils.bool()) {
            final var modifier = itemModifierDao.getRandomModifier(rarity);
            modifiers.add(modifier);
            if (RandomUtils.bool()) {
                // Может быть либо 2 префиксных, либо 1 суффикс и 1 префикс
                if (modifier.type() == ModifierType.SUFFIX) {
                    modifiers.add(itemModifierDao.getRandomModifierWithType(ModifierType.PREFIX, rarity));
                } else {
                    modifiers.add(itemModifierDao.getRandomModifierExcludeId(modifier.id(), rarity));
                }
            }
        }
        final var tempItem = new Item(
            0L,
            object.toItemObject(),
            rarity,
            modifiers.stream().map(GenerateModifier::toModifier).toList(),
            Optional.of(personage.id()),
            false,
            characteristicService.createCharacteristics(rarity, object, modifiers)
        );

        if (!personage.hasSpaceInBag(getPersonageItems(personage.id()))) {
            // TODO возможно стоит сохранять неудачные предметы в базу
            logger.info("Personage '{}' has no space in bag", personage.id().value());
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

    public Either<DropItemError, Item> canDropItem(Personage personage, long itemId) {
        final var itemResult = itemDao.getById(itemId);
        if (itemResult.isEmpty()) {
            logger.debug("Personage {} tried to drop incorrect item with id {}", personage.id(), itemId);
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
            .peek(_ -> itemDao.deletePersonageAndMakeEquipFalse(itemId))
            .map(_ -> itemDao.getById(itemId).orElseThrow());
    }

    private ItemRarity calculateRandomRarity() {
        int probability = RandomUtils.getInInterval(0, 100);
        return ItemRarity.COMMON;
    }
}
