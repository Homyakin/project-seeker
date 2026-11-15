package ru.homyakin.seeker.game.personage.power;

import java.util.List;
import ru.homyakin.seeker.game.item.models.Item;

/**
 * Read-only display rating. Game mechanics must never consume this result.
 */
public final class ReferencePowerCalculator {
    private ReferencePowerCalculator() {
    }

    public static ReferencePower calculate(List<Item> items, double displayScale) {
        if (items == null || items.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("Items must be specified");
        }
        if (!Double.isFinite(displayScale) || displayScale <= 0) {
            throw new IllegalArgumentException("Display scale must be finite and positive: " + displayScale);
        }

        final var equipmentPower = EquipmentPowerCalculator.calculate(items);
        final var displayed = equipmentPower.unscaled() * displayScale;
        if (!Double.isFinite(displayed)) {
            throw new IllegalArgumentException("Reference power result must be finite");
        }
        return new ReferencePower(
            equipmentPower.survivability(),
            equipmentPower.normalDamagePerTime(),
            equipmentPower.unscaled(),
            displayed
        );
    }
}
