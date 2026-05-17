package ru.homyakin.seeker.game.item;

import io.vavr.control.Either;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.item.database.ItemDao;
import ru.homyakin.seeker.game.item.database.ItemModifierDao;
import ru.homyakin.seeker.game.item.database.ItemObjectDao;
import java.util.Comparator;
import ru.homyakin.seeker.game.item.errors.EnhanceItemError;
import ru.homyakin.seeker.game.item.errors.GenerateItemError;
import ru.homyakin.seeker.game.item.errors.PutOnItemError;
import ru.homyakin.seeker.game.item.errors.TakeOffItemError;
import ru.homyakin.seeker.game.item.models.GenerateItemParams;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.item.models.PersonageItem;
import ru.homyakin.seeker.game.item.modifier.ItemModifierService;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;

@Service
public class ItemService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ItemObjectDao itemObjectDao;
    private final ItemModifierService itemModifierService;
    private final ItemDao itemDao;

    public ItemService(ItemObjectDao itemObjectDao, ItemModifierService itemModifierService, ItemDao itemDao) {
        this.itemObjectDao = itemObjectDao;
        this.itemModifierService = itemModifierService;
        this.itemDao = itemDao;
    }

    public Optional<PersonageItem> getById(long id) {
        return itemDao.getById(id);
    }

    public Either<GenerateItemError, PersonageItem> generateItemForPersonage(Personage personage, GenerateItemParams params) {
        final var objectRow = itemObjectDao.getRandomObject(params.slot());
        final var modifierRow = itemModifierService.pickModifier(params.rarity(), objectRow.object(), params.slot());
        final var tempItem = new PersonageItem(
            0L,
            objectRow.id(),
            objectRow.object(),
            modifierRow.map(ItemModifierDao.ModifierRow::id),
            modifierRow.map(ItemModifierDao.ModifierRow::modifier),
            params.rarity(),
            Optional.of(personage.id()),
            false
        );

        if (!personage.hasSpaceInBagForItems(getPersonageItems(personage.id()))) {
            logger.info("Personage '{}' has no space in bag", personage.id().value());
            final var itemWithoutPersonage = new PersonageItem(
                tempItem.id(),
                tempItem.objectId(),
                tempItem.object(),
                tempItem.modifierId(),
                tempItem.modifier(),
                tempItem.rarity(),
                Optional.empty(),
                tempItem.isEquipped()
            );
            final var id = itemDao.save(itemWithoutPersonage);
            return Either.left(new GenerateItemError.NotEnoughSpace(getById(id).orElseThrow()));
        }

        final var id = itemDao.save(tempItem);
        return Either.right(getById(id).orElseThrow());
    }

    public List<PersonageItem> getPersonageItems(PersonageId personageId) {
        return itemDao.getByPersonageId(personageId);
    }

    public Optional<PersonageItem> getPersonageItem(PersonageId personageId, long itemId) {
        return itemDao.getById(itemId)
            .filter(item -> item.personageId().map(it -> it.equals(personageId)).orElse(false));
    }

    public Optional<PersonageItem> removeItem(PersonageId personageId, long itemId) {
        return getPersonageItem(personageId, itemId)
            .map(_ -> {
                itemDao.deletePersonageAndMakeEquipFalse(itemId);
                return itemDao.getById(itemId).orElseThrow();
            });
    }

    public Either<PutOnItemError, PersonageItem> putOnItem(Personage personage, long itemId) {
        final var itemResult = getPersonageItem(personage.id(), itemId);
        if (itemResult.isEmpty()) {
            logger.debug("Personage {} tried to equip incorrect item with id {}", personage.id(), itemId);
            return Either.left(PutOnItemError.PersonageMissingItem.INSTANCE);
        }

        return personage.canPutOnItem(getPersonageItems(personage.id()), itemResult.get())
            .peek(_ -> itemDao.invertEquip(itemId))
            .map(_ -> getById(itemId).orElseThrow());
    }

    public Either<TakeOffItemError, PersonageItem> takeOffItem(Personage personage, long itemId) {
        final var itemResult = getPersonageItem(personage.id(), itemId);
        if (itemResult.isEmpty()) {
            logger.debug("Personage {} tried to take off incorrect item with id {}", personage.id(), itemId);
            return Either.left(TakeOffItemError.PersonageMissingItem.INSTANCE);
        }

        return personage.canTakeOffItem(getPersonageItems(personage.id()), itemResult.get())
            .peek(_ -> itemDao.invertEquip(itemId))
            .map(_ -> getById(itemId).orElseThrow());
    }

    public Either<EnhanceItemError, PersonageItem> enhance(PersonageItem item) {
        final var nextRarity = item.rarity().next();
        if (nextRarity.isEmpty()) {
            return Either.left(EnhanceItemError.MaxRarity.INSTANCE);
        }

        if (item.rarity() == ItemRarity.COMMON) {
            final var modifierRow = itemModifierService.pickModifier(item.object(), primarySlot(item.object()));
            itemDao.updateEnhancement(item.id(), modifierRow.id(), ItemRarity.UNCOMMON);
        } else {
            itemDao.updateEnhancement(
                item.id(),
                item.modifierId().orElse(null),
                nextRarity.get()
            );
        }
        return Either.right(getById(item.id()).orElseThrow());
    }

    private PersonageSlot primarySlot(ItemObject object) {
        return object.slots().stream()
            .min(Comparator.comparingInt(slot -> slot.id))
            .orElseThrow();
    }
}
