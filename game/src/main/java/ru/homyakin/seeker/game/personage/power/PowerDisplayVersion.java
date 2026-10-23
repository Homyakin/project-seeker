package ru.homyakin.seeker.game.personage.power;

import java.util.List;
import ru.homyakin.seeker.game.item.models.Item;

/**
 * Versioned presentation scale, never a combat or reward parameter.
 */
public enum PowerDisplayVersion {
    LEGACY_V1,
    REFERENCE_V1;

    /**
     * COMMON +0 sword, shield and the five original leather armor objects.
     * The scale is frozen rather than recalculated from mutable catalog data at startup.
     */
    public static final double REFERENCE_V1_SCALE = 1_271_759_375.0 / 383_571_429_696.0;
    public static final List<String> REFERENCE_V1_ITEM_CODES = List.of(
        "sword", "shield", "breastplate", "leather_chausses", "boots", "leather_helm", "leather_gloves"
    );

    public int referenceValue(List<Item> items) {
        if (this != REFERENCE_V1) {
            throw new IllegalStateException("Legacy display requires the legacy power calculation");
        }
        final var value = ReferencePowerCalculator.calculate(items, REFERENCE_V1_SCALE).displayed();
        final var rounded = Math.round(value);
        if (rounded < 0 || rounded > Integer.MAX_VALUE) {
            throw new ArithmeticException("Displayed power is outside the supported integer range: " + value);
        }
        return (int) rounded;
    }
}
