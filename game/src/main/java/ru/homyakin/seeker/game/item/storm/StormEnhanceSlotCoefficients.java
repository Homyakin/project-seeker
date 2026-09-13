package ru.homyakin.seeker.game.item.storm;

import java.util.Map;
import java.util.Set;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;

/**
 * Stable slot coefficients stored as integer quarters.
 */
public final class StormEnhanceSlotCoefficients {
    private static final Map<PersonageSlot, Integer> QUARTERS = Map.of(
        PersonageSlot.MAIN_HAND, 16,
        PersonageSlot.OFF_HAND, 5,
        PersonageSlot.BODY, 10,
        PersonageSlot.PANTS, 7,
        PersonageSlot.SHOES, 4,
        PersonageSlot.HELMET, 4,
        PersonageSlot.GLOVES, 4
    );
    private static final int MAX_QUARTERS = QUARTERS.values().stream().mapToInt(Integer::intValue).sum();

    private StormEnhanceSlotCoefficients() {
    }

    public static int quarters(Set<PersonageSlot> slots) {
        if (slots == null || slots.isEmpty()) {
            throw new IllegalArgumentException("Item slots must not be empty");
        }
        return slots.stream()
            .mapToInt(slot -> {
                final var quarters = QUARTERS.get(slot);
                if (quarters == null) {
                    throw new IllegalArgumentException("Unknown item slot: " + slot);
                }
                return quarters;
            })
            .sum();
    }

    public static int maxQuarters() {
        return MAX_QUARTERS;
    }
}
