package ru.homyakin.seeker.game.item.catalog;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.storm.ItemProgression;

class ScalingV4CatalogRevisionTest {
    private static final LoadedEquipmentCatalog V3 = EquipmentCatalogLoader.loadValidated(
        EquipmentCatalogVersion.SCALING_V3
    );
    private static final LoadedEquipmentCatalog V4 = EquipmentCatalogLoader.loadValidated(
        EquipmentCatalogVersion.SCALING_V4
    );
    private static final Map<String, Integer> REVISED_SPEEDS = Map.of(
        "ceremonial_tunic", 40,
        "ceremonial_trousers", 30
    );

    @Test
    void fourthCatalogRevisionChangesOnlyTwoCeremonialItemSpeeds() {
        Assertions.assertEquals(V3.modifiers(), V4.modifiers());
        Assertions.assertEquals(V3.defaultItems(), V4.defaultItems());
        final var v3Objects = objectsByCode(V3);
        final var v4Objects = objectsByCode(V4);
        Assertions.assertEquals(v3Objects.keySet(), v4Objects.keySet());

        for (final var entry : v3Objects.entrySet()) {
            final var source = entry.getValue();
            final var expectedSpeed = REVISED_SPEEDS.get(entry.getKey());
            final var expected = expectedSpeed == null ? source : copyWithSpeed(source, expectedSpeed);
            Assertions.assertEquals(expected, v4Objects.get(entry.getKey()), entry.getKey());
        }
        Assertions.assertEquals(34, v3Objects.get("ceremonial_tunic").speed());
        Assertions.assertEquals(24, v3Objects.get("ceremonial_trousers").speed());
        Assertions.assertEquals(
            V3.itemObjects().stream().map(item -> REVISED_SPEEDS.containsKey(item.code())
                ? copyWithSpeed(item, REVISED_SPEEDS.get(item.code())) : item).toList(),
            V4.itemObjects()
        );
    }

    @Test
    void speedCorrectionDoesNotChangeAnyEnhancementDelta() {
        final var v3Objects = objectsByCode(V3);
        final var v4Objects = objectsByCode(V4);
        for (final var code : REVISED_SPEEDS.keySet()) {
            final var source = v3Objects.get(code);
            final var revised = v4Objects.get(code);
            for (int level = 0; level <= 21; level++) {
                final var sourceState = ItemProgression.state(source, level);
                final var revisedState = ItemProgression.state(revised, level);
                Assertions.assertEquals(sourceState, revisedState, code + " +" + level);
                if (level < 21) {
                    Assertions.assertEquals(
                        ItemProgression.transition(source, level),
                        ItemProgression.transition(revised, level),
                        code + " +" + level + " transition"
                    );
                }
            }
        }
    }

    @Test
    void completeCeremonialArmorKeepsItsExplicitDodgeTradeoff() {
        final var objects = objectsByCode(V4);
        final var oldCloth = List.of("robe", "cloth_chausses", "cloth_boots", "hood", "cloth_gloves")
            .stream().map(objects::get).toList();
        final var ceremonial = List.of(
            "ceremonial_tunic", "ceremonial_trousers", "ceremonial_slippers", "ceremonial_veil", "ceremonial_gloves"
        ).stream().map(objects::get).toList();

        Assertions.assertAll(
            () -> Assertions.assertEquals(132, oldCloth.stream().mapToInt(ItemObject::speed).sum()),
            () -> Assertions.assertEquals(138, ceremonial.stream().mapToInt(ItemObject::speed).sum()),
            () -> Assertions.assertEquals(10, oldCloth.stream().mapToInt(ItemObject::dodgeChance).sum()),
            () -> Assertions.assertEquals(5, ceremonial.stream().mapToInt(ItemObject::dodgeChance).sum()),
            () -> Assertions.assertEquals(4, oldCloth.stream().mapToInt(ItemObject::critChance).sum()),
            () -> Assertions.assertEquals(9, ceremonial.stream().mapToInt(ItemObject::critChance).sum()),
            () -> Assertions.assertEquals(0.09, oldCloth.stream().mapToDouble(ItemObject::critMultiplier).sum(), 1e-10),
            () -> Assertions.assertEquals(0.22, ceremonial.stream().mapToDouble(ItemObject::critMultiplier).sum(), 1e-10),
            () -> Assertions.assertEquals(5, oldCloth.stream().mapToInt(ItemObject::impact).sum()),
            () -> Assertions.assertEquals(10, ceremonial.stream().mapToInt(ItemObject::impact).sum()),
            () -> Assertions.assertEquals(
                oldCloth.stream().mapToInt(ItemObject::health).sum(),
                ceremonial.stream().mapToInt(ItemObject::health).sum()
            ),
            () -> Assertions.assertEquals(
                oldCloth.stream().map(ItemObject::defense).toList(),
                ceremonial.stream().map(ItemObject::defense).toList()
            )
        );
    }

    private static Map<String, ItemObject> objectsByCode(LoadedEquipmentCatalog catalog) {
        return catalog.itemObjects().stream().collect(Collectors.toUnmodifiableMap(
            ItemObject::code,
            Function.identity()
        ));
    }

    private static ItemObject copyWithSpeed(ItemObject source, int speed) {
        return new ItemObject(
            source.code(),
            source.slots(),
            source.attacks(),
            source.defense(),
            source.health(),
            source.critChance(),
            source.dodgeChance(),
            source.critMultiplier(),
            speed,
            source.baseThreat(),
            source.impact(),
            source.progressionVersion(),
            source.locales()
        );
    }
}
