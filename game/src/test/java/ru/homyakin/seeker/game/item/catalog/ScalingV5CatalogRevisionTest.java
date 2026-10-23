package ru.homyakin.seeker.game.item.catalog;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.ItemAttack;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.storm.ItemProgression;

class ScalingV5CatalogRevisionTest {
    private static final LoadedEquipmentCatalog V4 = EquipmentCatalogLoader.loadValidated(
        EquipmentCatalogVersion.SCALING_V4
    );
    private static final LoadedEquipmentCatalog V5 = EquipmentCatalogLoader.loadValidated(
        EquipmentCatalogVersion.SCALING_V5
    );

    @Test
    void fifthCatalogRevisionChangesOnlyTheHalberdAttack() {
        Assertions.assertEquals(V4.modifiers(), V5.modifiers());
        Assertions.assertEquals(V4.defaultItems(), V5.defaultItems());
        final var v4Objects = objectsByCode(V4);
        final var v5Objects = objectsByCode(V5);
        Assertions.assertEquals(v4Objects.keySet(), v5Objects.keySet());
        Assertions.assertEquals(List.of(new ItemAttack(AttackType.PIERCE, 1, 2, 360)),
            v4Objects.get("halberd").attacks());
        final var revisedAttacks = List.of(new ItemAttack(AttackType.PIERCE, 1, 2, 420));
        Assertions.assertEquals(
            V4.itemObjects().stream()
                .map(item -> item.code().equals("halberd") ? copyWithAttacks(item, revisedAttacks) : item)
                .toList(),
            V5.itemObjects()
        );
        Assertions.assertEquals(40, v5Objects.get("ceremonial_tunic").speed());
        Assertions.assertEquals(30, v5Objects.get("ceremonial_trousers").speed());
    }

    @Test
    void halberdTradesThirdLineReachForOneGrowingBaseStepOfDamage() {
        final var objects = objectsByCode(V5);
        final var halberd = objects.get("halberd");
        final var crossbow = objects.get("crossbow");
        final var halberdAttack = halberd.attacks().getFirst();
        final var crossbowAttack = crossbow.attacks().getFirst();

        Assertions.assertEquals(List.of(new ItemAttack(AttackType.PIERCE, 1, 3, 360)), crossbow.attacks());
        Assertions.assertEquals(crossbow.slots(), halberd.slots());
        Assertions.assertEquals(crossbowAttack.attackType(), halberdAttack.attackType());
        Assertions.assertTrue(halberdAttack.isAvailableAt(1));
        Assertions.assertTrue(halberdAttack.isAvailableAt(2));
        Assertions.assertFalse(halberdAttack.isAvailableAt(3));
        Assertions.assertTrue(crossbowAttack.isAvailableAt(3));
        Assertions.assertEquals(60, halberdAttack.attack() - crossbowAttack.attack());
        Assertions.assertEquals(480, objects.get("two_handed_sword").attacks().getFirst().attack());
        Assertions.assertEquals(480, objects.get("command_staff").attacks().getFirst().attack());
    }

    @Test
    void halberdKeepsExactPositiveGrowthAndItsDamageTradeoffThroughLevel75() {
        final var oldHalberd = objectsByCode(V4).get("halberd");
        final var objects = objectsByCode(V5);
        final var halberd = objects.get("halberd");
        final var crossbow = objects.get("crossbow");
        final var sword = objects.get("two_handed_sword");
        final var commandStaff = objects.get("command_staff");
        Assertions.assertTrue(ItemProgression.maxSupportedLevel(halberd) >= 75);

        for (int level = 0; level <= 75; level++) {
            final var state = ItemProgression.state(halberd, level);
            final var attack = state.attacks().getFirst();
            final var oldAttack = ItemProgression.state(oldHalberd, level).attacks().getFirst();
            final var crossbowAttack = ItemProgression.state(crossbow, level).attacks().getFirst();
            final var swordAttack = ItemProgression.state(sword, level).attacks().getFirst();
            final var commandAttack = ItemProgression.state(commandStaff, level).attacks().getFirst();
            final var label = "halberd +" + level;
            Assertions.assertEquals(List.of(new ItemAttack(AttackType.PIERCE, 1, 2, 420 + 7 * level)),
                state.attacks(), label);
            Assertions.assertEquals(60 + level, attack.attack() - oldAttack.attack(), label);
            Assertions.assertTrue(attack.attack() > crossbowAttack.attack(), label);
            Assertions.assertTrue(attack.attack() < swordAttack.attack(), label);
            Assertions.assertTrue(attack.attack() < commandAttack.attack(), label);
            Assertions.assertEquals(0, state.health(), label);
            Assertions.assertTrue(state.defense().isEmpty(), label);
            if (level < 75) {
                Assertions.assertEquals(
                    new ItemProgression.Delta(0,
                        List.of(new ItemProgression.AttackDelta(AttackType.PIERCE, 1, 2, 7)),
                        Optional.empty()),
                    ItemProgression.transition(halberd, level), label
                );
            }
        }
        Assertions.assertEquals(945, ItemProgression.state(halberd, 75).attacks().getFirst().attack());
    }

    private static Map<String, ItemObject> objectsByCode(LoadedEquipmentCatalog catalog) {
        return catalog.itemObjects().stream().collect(Collectors.toUnmodifiableMap(
            ItemObject::code,
            Function.identity()
        ));
    }

    private static ItemObject copyWithAttacks(ItemObject source, List<ItemAttack> attacks) {
        return new ItemObject(
            source.code(),
            source.slots(),
            attacks,
            source.defense(),
            source.health(),
            source.critChance(),
            source.dodgeChance(),
            source.critMultiplier(),
            source.speed(),
            source.baseThreat(),
            source.impact(),
            source.progressionVersion(),
            source.locales()
        );
    }
}
