package ru.homyakin.seeker.game.item.loadout.action;

import io.vavr.control.Either;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.homyakin.seeker.game.item.ItemService;
import ru.homyakin.seeker.game.item.database.ItemDao;
import ru.homyakin.seeker.game.item.loadout.entity.ApplyLoadoutError;
import ru.homyakin.seeker.game.item.loadout.entity.CreateLoadoutError;
import ru.homyakin.seeker.game.item.loadout.entity.DeleteLoadoutError;
import ru.homyakin.seeker.game.item.loadout.entity.EquipmentLoadout;
import ru.homyakin.seeker.game.item.loadout.entity.LoadoutNameValidator;
import ru.homyakin.seeker.game.item.loadout.entity.RenameLoadoutError;
import ru.homyakin.seeker.game.item.loadout.entity.SaveLoadoutError;
import ru.homyakin.seeker.game.item.loadout.infra.postgres.EquipmentLoadoutDao;
import ru.homyakin.seeker.game.item.models.Inventory;
import ru.homyakin.seeker.game.item.models.PersonageItem;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.utils.models.Success;

@Service
public class EquipmentLoadoutService {
    public static final int MAX_LOADOUTS = 3;

    private final EquipmentLoadoutDao loadoutDao;
    private final ItemService itemService;
    private final ItemDao itemDao;

    public EquipmentLoadoutService(
        EquipmentLoadoutDao loadoutDao,
        ItemService itemService,
        ItemDao itemDao
    ) {
        this.loadoutDao = loadoutDao;
        this.itemService = itemService;
        this.itemDao = itemDao;
    }

    public List<EquipmentLoadout> list(PersonageId personageId) {
        return loadoutDao.findByPersonageId(personageId);
    }

    public Optional<EquipmentLoadout> get(PersonageId personageId, long loadoutId) {
        return loadoutDao.findById(loadoutId)
            .filter(loadout -> loadout.personageId().equals(personageId));
    }

    public List<EquipmentLoadout> findByItemId(PersonageId personageId, long itemId) {
        return loadoutDao.findByPersonageIdAndItemId(personageId, itemId);
    }

    public boolean canCreate(PersonageId personageId) {
        return loadoutDao.countByPersonageId(personageId) < MAX_LOADOUTS;
    }

    @Transactional
    public Either<CreateLoadoutError, EquipmentLoadout> createFromCurrent(PersonageId personageId, String name) {
        final var validatedName = LoadoutNameValidator.validate(name);
        if (validatedName.isLeft()) {
            return Either.left(new CreateLoadoutError.InvalidName(validatedName.getLeft()));
        }
        if (!canCreate(personageId)) {
            return Either.left(CreateLoadoutError.MaxLoadoutsReached.INSTANCE);
        }
        final var itemIds = currentEquippedItemIds(personageId);
        final var id = loadoutDao.insert(personageId, validatedName.get(), itemIds);
        return Either.right(loadoutDao.findById(id).orElseThrow());
    }

    @Transactional
    public Either<SaveLoadoutError, EquipmentLoadout> saveCurrent(PersonageId personageId, long loadoutId) {
        final var loadout = get(personageId, loadoutId);
        if (loadout.isEmpty()) {
            return Either.left(SaveLoadoutError.LoadoutNotFound.INSTANCE);
        }
        final var itemIds = currentEquippedItemIds(personageId);
        loadoutDao.updateItemIds(loadoutId, itemIds);
        return Either.right(loadoutDao.findById(loadoutId).orElseThrow());
    }

    @Transactional
    public Either<ApplyLoadoutError, Success> apply(PersonageId personageId, long loadoutId) {
        final var loadoutResult = get(personageId, loadoutId);
        if (loadoutResult.isEmpty()) {
            return Either.left(ApplyLoadoutError.LoadoutNotFound.INSTANCE);
        }
        final var loadout = loadoutResult.get();
        final var inventory = itemService.getPersonageItems(personageId);
        final var ownedById = inventory.items().stream()
            .collect(Collectors.toMap(PersonageItem::id, item -> item));

        final var missingItemIds = loadout.itemIds().stream()
            .filter(id -> !ownedById.containsKey(id))
            .toList();
        if (!missingItemIds.isEmpty()) {
            return Either.left(new ApplyLoadoutError.MissingItems(missingItemIds));
        }

        final var targetItems = loadout.itemIds().stream()
            .map(ownedById::get)
            .toList();
        if (hasSlotConflicts(targetItems)) {
            return Either.left(ApplyLoadoutError.ConflictingSlots.INSTANCE);
        }

        final var totalOwned = inventory.items().size();
        final var targetCount = targetItems.size();
        final var unequippedCount = totalOwned - targetCount;
        if (unequippedCount > Inventory.maxBagSize()) {
            return Either.left(ApplyLoadoutError.NotEnoughSpaceInBag.INSTANCE);
        }

        itemDao.setEquippedForPersonage(personageId, loadout.itemIds());
        return Either.right(Success.INSTANCE);
    }

    @Transactional
    public Either<RenameLoadoutError, EquipmentLoadout> rename(PersonageId personageId, long loadoutId, String name) {
        final var validatedName = LoadoutNameValidator.validate(name);
        if (validatedName.isLeft()) {
            return Either.left(new RenameLoadoutError.InvalidName(validatedName.getLeft()));
        }
        if (get(personageId, loadoutId).isEmpty()) {
            return Either.left(RenameLoadoutError.LoadoutNotFound.INSTANCE);
        }
        loadoutDao.updateName(loadoutId, validatedName.get());
        return Either.right(loadoutDao.findById(loadoutId).orElseThrow());
    }

    @Transactional
    public Either<DeleteLoadoutError, Success> delete(PersonageId personageId, long loadoutId) {
        if (get(personageId, loadoutId).isEmpty()) {
            return Either.left(DeleteLoadoutError.LoadoutNotFound.INSTANCE);
        }
        loadoutDao.delete(loadoutId);
        return Either.right(Success.INSTANCE);
    }

    @Transactional
    public List<String> removeItemFromLoadouts(PersonageId personageId, long itemId) {
        final var affected = findByItemId(personageId, itemId);
        final var names = new ArrayList<String>();
        for (final var loadout : affected) {
            final var updatedIds = loadout.itemIds().stream()
                .filter(id -> id != itemId)
                .toList();
            loadoutDao.updateItemIds(loadout.id(), updatedIds);
            names.add(loadout.name());
        }
        return names;
    }

    private List<Long> currentEquippedItemIds(PersonageId personageId) {
        return itemService.getPersonageItems(personageId).items().stream()
            .filter(PersonageItem::isEquipped)
            .map(PersonageItem::id)
            .toList();
    }

    private boolean hasSlotConflicts(List<PersonageItem> items) {
        final Set<PersonageSlot> occupied = new HashSet<>();
        for (final var item : items) {
            for (final var slot : item.object().slots()) {
                if (!occupied.add(slot)) {
                    return true;
                }
            }
        }
        return false;
    }
}
