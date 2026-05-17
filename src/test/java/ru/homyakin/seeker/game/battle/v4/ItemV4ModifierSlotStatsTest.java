package ru.homyakin.seeker.game.battle.v4;

import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.utils.ResourceUtils;

import java.util.EnumMap;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Loads {@code game-data/item_v4_modifier.toml} and prints how many modifiers are available per slot.
 */
class ItemV4ModifierSlotStatsTest {
    private static final String TOML_PATH = "game-data/item_v4_modifier.toml";

    @Test
    void printModifierCountBySlot() {
        final var modifiers = loadModifiers();
        final var countBySlot = new EnumMap<PersonageSlot, Integer>(PersonageSlot.class);
        for (final var slot : PersonageSlot.values()) {
            countBySlot.put(slot, 0);
        }
        for (final var modifier : modifiers) {
            for (final var slot : modifier.availableOnSlots()) {
                countBySlot.merge(slot, 1, Integer::sum);
            }
        }

        System.out.println("modifiers by slot:");
        for (final var slot : PersonageSlot.values()) {
            final var codes = modifiers.stream()
                .filter(modifier -> modifier.availableOnSlots().contains(slot))
                .map(Modifier::code)
                .sorted()
                .collect(Collectors.joining(", "));
            System.out.printf("  %s: %d (%s)%n", slot, countBySlot.get(slot), codes);
        }

        assertFalse(modifiers.isEmpty());
    }

    private static List<Modifier> loadModifiers() {
        return ResourceUtils.calc(TOML_PATH, ItemV4ModifierToml::load)
            .orElseThrow(() -> new IllegalStateException("Missing " + TOML_PATH))
            .modifiers();
    }
}
