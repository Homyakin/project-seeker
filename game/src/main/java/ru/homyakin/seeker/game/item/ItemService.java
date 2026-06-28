package ru.homyakin.seeker.game.item;

import io.vavr.control.Either;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.item.database.ItemDao;
import ru.homyakin.seeker.game.item.database.ItemObjectDao;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import ru.homyakin.seeker.game.item.errors.EnhanceItemError;
import ru.homyakin.seeker.game.item.errors.GenerateItemError;
import ru.homyakin.seeker.game.item.errors.PutOnItemError;
import ru.homyakin.seeker.game.item.errors.TakeOffItemError;
import ru.homyakin.seeker.game.item.models.CatalogItemObject;
import ru.homyakin.seeker.game.item.models.CatalogModifier;
import ru.homyakin.seeker.game.item.models.DefaultItems;
import ru.homyakin.seeker.game.item.models.GenerateItemParams;
import ru.homyakin.seeker.game.item.models.Inventory;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.PutOnItemResult;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.item.models.PersonageItem;
import ru.homyakin.seeker.game.item.modifier.ItemModifierService;
import ru.homyakin.seeker.game.personage.models.Characteristics;
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

    public Either<GenerateItemError, PersonageItem> generateItemForPersonage(
        Personage personage,
        GenerateItemParams params
    ) {
        final var catalogItemObject = itemObjectDao.getRandomObject(params.slot());
        final var catalogModifier = itemModifierService.pickModifier(
            params.rarity(),
            catalogItemObject.object(),
            params.slot()
        );
        return generateItemForPersonage(personage, params.rarity(), catalogModifier, catalogItemObject);
    }

    public Either<GenerateItemError, PersonageItem> generateItemForPersonage(
        Personage personage,
        CatalogItemObject object
    ) {
        return generateItemForPersonage(personage, ItemRarity.COMMON, Optional.empty(), object);
    }

    private Either<GenerateItemError, PersonageItem> generateItemForPersonage(
        Personage personage,
        ItemRarity rarity,
        Optional<CatalogModifier> modifier,
        CatalogItemObject catalogObject
    ) {
        final var tempItem = new PersonageItem(
            0L,
            catalogObject.id(),
            catalogObject.object(),
            modifier.map(CatalogModifier::id),
            modifier.map(CatalogModifier::modifier),
            rarity,
            Optional.of(personage.id()),
            false
        );

        if (!getPersonageItems(personage.id()).hasSpaceInBag()) {
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

    public Inventory getPersonageItems(PersonageId personageId) {
        return itemDao.getByPersonageId(personageId);
    }

    public Map<PersonageId, List<Item>> getEquippedItemsByPersonageIds(Set<PersonageId> personageIds) {
        if (personageIds.isEmpty()) {
            return Map.of();
        }
        final var equippedByPersonageId = itemDao.getEquippedByPersonageIds(personageIds);
        return personageIds.stream()
            .collect(Collectors.toMap(
                id -> id,
                id -> equippedItemsWithDefaults(equippedByPersonageId.getOrDefault(id, List.of()))
            ));
    }

    public Map<PersonageId, Characteristics> getEquippedCharacteristicsByPersonageIds(Set<PersonageId> personageIds) {
        if (personageIds.isEmpty()) {
            return Map.of();
        }
        final var equippedByPersonageId = itemDao.getEquippedByPersonageIds(personageIds);
        return personageIds.stream()
            .collect(Collectors.toMap(
                id -> id,
                id -> calculateEquippedCharacteristics(equippedByPersonageId.getOrDefault(id, List.of()))
            ));
    }

    private Characteristics calculateEquippedCharacteristics(List<PersonageItem> equippedItems) {
        final var total = equippedItems.stream()
            .map(PersonageItem::toItem)
            .map(Item::visibleCharacteristics)
            .collect(Characteristics::empty, Characteristics::add, Characteristics::add);
        final var occupiedSlots = equippedItems.stream()
            .flatMap(item -> item.object().slots().stream())
            .collect(Collectors.toSet());
        return total.add(DefaultItems.characteristicsForFreeSlots(occupiedSlots));
    }

    private List<Item> equippedItemsWithDefaults(List<PersonageItem> equippedItems) {
        final var items = equippedItems.stream()
            .map(PersonageItem::toItem)
            .collect(Collectors.toCollection(ArrayList::new));
        final var occupiedSlots = equippedItems.stream()
            .flatMap(item -> item.object().slots().stream())
            .collect(Collectors.toSet());
        for (final var slot : PersonageSlot.values()) {
            DefaultItems.defaultItemForSlot(slot, occupiedSlots).ifPresent(items::add);
        }
        return items;
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

    public Either<PutOnItemError, PutOnItemResult> putOnItem(Personage personage, long itemId) {
        final var itemResult = getPersonageItem(personage.id(), itemId);
        if (itemResult.isEmpty()) {
            logger.debug("Personage {} tried to equip incorrect item with id {}", personage.id(), itemId);
            return Either.left(PutOnItemError.PersonageMissingItem.INSTANCE);
        }

        return getPersonageItems(personage.id()).canPutOnItem(personage.id(), itemResult.get())
            .map(itemsToTakeOff -> {
                for (final var itemToTakeOff : itemsToTakeOff) {
                    itemDao.invertEquip(itemToTakeOff.id());
                }
                itemDao.invertEquip(itemId);
                final var takenOffItems = itemsToTakeOff.stream()
                    .map(item -> getById(item.id()).orElseThrow())
                    .toList();
                return new PutOnItemResult(getById(itemId).orElseThrow(), takenOffItems);
            });
    }

    public Either<TakeOffItemError, PersonageItem> takeOffItem(Personage personage, long itemId) {
        final var itemResult = getPersonageItem(personage.id(), itemId);
        if (itemResult.isEmpty()) {
            logger.debug("Personage {} tried to take off incorrect item with id {}", personage.id(), itemId);
            return Either.left(TakeOffItemError.PersonageMissingItem.INSTANCE);
        }

        return getPersonageItems(personage.id()).canTakeOffItem(personage.id(), itemResult.get())
            .peek(_ -> itemDao.invertEquip(itemId))
            .map(_ -> getById(itemId).orElseThrow());
    }

    public Either<EnhanceItemError, PersonageItem> enhance(PersonageItem item) {
        final var nextRarity = item.rarity().next();
        if (nextRarity.isEmpty()) {
            return Either.left(EnhanceItemError.MaxRarity.INSTANCE);
        }

        if (item.rarity() == ItemRarity.COMMON) {
            final var catalogModifier = itemModifierService.pickModifier(item.object(), primarySlot(item.object()));
            itemDao.updateEnhancement(item.id(), catalogModifier.id(), ItemRarity.UNCOMMON);
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
