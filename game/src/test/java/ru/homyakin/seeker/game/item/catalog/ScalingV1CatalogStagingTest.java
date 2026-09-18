package ru.homyakin.seeker.game.item.catalog;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.item.models.DefenseType;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.Modifier;
import ru.homyakin.seeker.game.item.modifier.ModifierCompatibility;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;

class ScalingV1CatalogStagingTest {
    private static final List<PersonageSlot> ARMOR_SLOTS = List.of(
        PersonageSlot.BODY,
        PersonageSlot.PANTS,
        PersonageSlot.SHOES,
        PersonageSlot.HELMET,
        PersonageSlot.GLOVES
    );

    @Test
    void stagedReleaseKeepsExactPreviousAcquisitionSet() {
        final var release = EquipmentCatalogLoader.loadValidated(EquipmentCatalogVersion.SCALING_V1);
        final var previousItems = previousItems();
        final var previousModifiers = previousModifiers();
        final var previousItemCodes = codes(previousItems, ItemObject::code);
        final var previousModifierCodes = codes(previousModifiers, Modifier::code);

        Assertions.assertEquals(40, previousItemCodes.size());
        Assertions.assertEquals(22, release.itemObjects().stream()
            .filter(item -> !previousItemCodes.contains(item.code()))
            .count());
        Assertions.assertEquals(11, previousModifierCodes.size());
        Assertions.assertEquals(4, release.modifiers().stream()
            .filter(modifier -> !previousModifierCodes.contains(modifier.code()))
            .count());

        final var stagedItems = release.itemObjects().stream()
            .filter(item -> previousItemCodes.contains(item.code()))
            .toList();
        final var stagedModifiers = release.modifiers().stream()
            .filter(modifier -> previousModifierCodes.contains(modifier.code()))
            .toList();
        for (final var item : stagedItems) {
            for (final var slot : item.slots()) {
                Assertions.assertTrue(
                    stagedModifiers.stream().anyMatch(modifier ->
                        ModifierCompatibility.isCompatible(item, modifier, slot)
                    ),
                    item.code() + "/" + slot
                );
            }
        }

        for (final var slot : ARMOR_SLOTS) {
            final var armors = stagedItems.stream()
                .filter(item -> item.slots().equals(Set.of(slot)))
                .toList();
            Assertions.assertEquals(4, armors.size(), slot.name());
            for (final var type : DefenseType.values()) {
                Assertions.assertEquals(
                    1,
                    armors.stream()
                        .flatMap(item -> item.defense().stream())
                        .filter(defense -> defense.defenseType() == type)
                        .count(),
                    slot + "/" + type
                );
            }
        }
    }

    @Test
    void allHistoricalAssignmentsRemainResolvableWithoutRevalidation() {
        final var previousItems = previousItems();
        final var previousModifiers = previousModifiers();
        final var release = EquipmentCatalogLoader.loadValidated(EquipmentCatalogVersion.SCALING_V1);
        final Map<String, ItemObject> releaseItems = release.itemObjects().stream()
            .collect(Collectors.toMap(ItemObject::code, Function.identity()));
        final Map<String, Modifier> releaseModifiers = release.modifiers().stream()
            .collect(Collectors.toMap(Modifier::code, Function.identity()));

        var historicalPairsOutsideNewSelection = 0;
        for (final var previousItem : previousItems) {
            Assertions.assertTrue(releaseItems.containsKey(previousItem.code()), previousItem.code());
            for (final var previousModifier : previousModifiers) {
                final boolean historicallyAssignable = previousItem.slots().stream()
                    .anyMatch(slot -> ModifierCompatibility.isCompatible(previousItem, previousModifier, slot));
                if (!historicallyAssignable) {
                    continue;
                }
                final var replacement = releaseModifiers.get(previousModifier.code());
                Assertions.assertNotNull(replacement, previousModifier.code());
                Assertions.assertEquals(previousModifier.activeEnum(), replacement.activeEnum(), previousModifier.code());
                final var replacementItem = releaseItems.get(previousItem.code());
                final boolean stillAssignable = replacementItem.slots().stream()
                    .anyMatch(slot -> ModifierCompatibility.isCompatible(replacementItem, replacement, slot));
                if (!stillAssignable) {
                    historicalPairsOutsideNewSelection++;
                }
            }
        }

        Assertions.assertEquals(60, historicalPairsOutsideNewSelection);
    }

    private static List<ItemObject> previousItems() {
        return load("game-data/item_objects_catalog.toml", ItemObjectsToml::load).itemObjects();
    }

    private static List<Modifier> previousModifiers() {
        return load("game-data/item_modifiers_catalog.toml", ItemModifiersToml::load).modifiers();
    }

    private static <T> Set<String> codes(List<T> values, Function<T, String> code) {
        return values.stream().map(code).collect(Collectors.toUnmodifiableSet());
    }

    private static <T> T load(String path, Function<InputStream, T> loader) {
        try (final var stream = ScalingV1CatalogStagingTest.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing test resource: " + path);
            }
            return loader.apply(stream);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Failed to close test resource: " + path, e);
        }
    }
}
