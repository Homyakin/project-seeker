package ru.homyakin.seeker.game.item.models;

import io.vavr.control.Either;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ru.homyakin.seeker.game.item.errors.PutOnItemError;
import ru.homyakin.seeker.game.item.errors.TakeOffItemError;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.utils.models.Success;

public record Inventory(
    List<PersonageItem> items
) {
    public static int maxBagSize() {
        return MAX_BAG_SIZE;
    }

    public boolean hasSpaceInBag() {
        return items.stream().filter(it -> !it.isEquipped()).count() < maxBagSize();
    }

    public Either<PutOnItemError, List<PersonageItem>> canPutOnItem(PersonageId personageId, PersonageItem item) {
        if (!item.personageId().map(it -> it.equals(personageId)).orElse(false)) {
            return Either.left(PutOnItemError.PersonageMissingItem.INSTANCE);
        }
        if (item.isEquipped()) {
            return Either.left(PutOnItemError.AlreadyEquipped.INSTANCE);
        }

        final var requiredSlots = item.object().slots();
        final var conflictingItems = new ArrayList<PersonageItem>();
        for (final var personageItem : items) {
            if (!personageItem.isEquipped()) {
                continue;
            }
            final var conflictsWithRequiredSlots = personageItem.object().slots().stream()
                .anyMatch(requiredSlots::contains);
            if (conflictsWithRequiredSlots && conflictingItems.stream().noneMatch(it -> it.id() == personageItem.id())) {
                conflictingItems.add(personageItem);
            }
        }
        if (conflictingItems.isEmpty()) {
            return Either.right(List.of());
        }

        final var itemsInBag = items.stream().filter(it -> !it.isEquipped()).count();
        if (itemsInBag - 1 + conflictingItems.size() > maxBagSize()) {
            final var occupiedSlots = conflictingItems.stream()
                .flatMap(conflictingItem -> conflictingItem.object().slots().stream())
                .filter(requiredSlots::contains)
                .distinct()
                .sorted(Comparator.comparingInt(slot -> slot.id))
                .toList();
            return Either.left(new PutOnItemError.NotEnoughSpaceOnPutOnItem(occupiedSlots));
        }
        return Either.right(conflictingItems);
    }

    public Either<TakeOffItemError, Success> canTakeOffItem(PersonageId personageId, PersonageItem item) {
        if (!item.personageId().map(it -> it.equals(personageId)).orElse(false)) {
            return Either.left(TakeOffItemError.PersonageMissingItem.INSTANCE);
        }
        if (!item.isEquipped()) {
            return Either.left(TakeOffItemError.AlreadyTakenOff.INSTANCE);
        }
        int itemsInBag = 0;
        for (final var personageItem : items) {
            if (personageItem.isEquipped()) {
                ++itemsInBag;
            }
        }

        if (itemsInBag >= maxBagSize()) {
            return Either.left(TakeOffItemError.NotEnoughSpaceInBag.INSTANCE);
        }
        return Either.right(Success.INSTANCE);
    }

    public List<PersonageSlot> getFreeSlots() {
        return getFreeSlotsFromEquipped(items.stream()
            .filter(PersonageItem::isEquipped)
            .map(item -> item.object().slots())
            .toList());
    }

    private List<PersonageSlot> getFreeSlotsFromEquipped(List<Set<PersonageSlot>> equippedItemSlots) {
        final var freeSlots = new HashMap<>(personageAvailableSlots);
        for (final var slots : equippedItemSlots) {
            for (final var slot : slots) {
                freeSlots.computeIfPresent(slot, (k, v) -> v - 1);
            }
        }
        final var result = new ArrayList<PersonageSlot>();
        for (final var entry : freeSlots.entrySet()) {
            for (int i = 0; i < entry.getValue(); ++i) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    private static final int MAX_BAG_SIZE = 15;
    private static final Map<PersonageSlot, Integer> personageAvailableSlots = new HashMap<>() {{
        put(PersonageSlot.MAIN_HAND, 1);
        put(PersonageSlot.OFF_HAND, 1);
        put(PersonageSlot.BODY, 1);
        put(PersonageSlot.PANTS, 1);
        put(PersonageSlot.HELMET, 1);
        put(PersonageSlot.GLOVES, 1);
        put(PersonageSlot.SHOES, 1);
    }};
}
