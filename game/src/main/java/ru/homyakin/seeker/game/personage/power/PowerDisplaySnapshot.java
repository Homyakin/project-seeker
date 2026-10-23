package ru.homyakin.seeker.game.personage.power;

import java.util.List;
import java.util.Optional;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemProgressionVersion;

/**
 * Presentation-only snapshot taken before temporary combat effects are applied.
 */
public record PowerDisplaySnapshot(PowerDisplayVersion version, int value) {
    public PowerDisplaySnapshot {
        if (version != PowerDisplayVersion.REFERENCE_V1 || value < 0) {
            throw new IllegalArgumentException("A reference display version and non-negative power are required");
        }
    }

    public static PowerDisplaySnapshot referenceV1(List<Item> items) {
        return new PowerDisplaySnapshot(PowerDisplayVersion.REFERENCE_V1, PowerDisplayVersion.REFERENCE_V1.referenceValue(items));
    }

    public static Optional<PowerDisplaySnapshot> forEquipment(List<Item> items) {
        if (items.isEmpty() || items.stream().anyMatch(item ->
            item.object().code() == null || item.object().progressionVersion() != ItemProgressionVersion.V1
        )) {
            return Optional.empty();
        }
        try {
            return Optional.of(referenceV1(items));
        } catch (IllegalArgumentException | ArithmeticException _) {
            // An unsupported display input must never change whether combat can be processed.
            return Optional.empty();
        }
    }
}
