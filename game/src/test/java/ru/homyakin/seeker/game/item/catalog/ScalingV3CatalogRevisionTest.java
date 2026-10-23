package ru.homyakin.seeker.game.item.catalog;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.ItemAttack;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.storm.ItemProgression;

class ScalingV3CatalogRevisionTest {
    private static final LoadedEquipmentCatalog V2 = EquipmentCatalogLoader.loadValidated(
        EquipmentCatalogVersion.SCALING_V2
    );
    private static final LoadedEquipmentCatalog V3 = EquipmentCatalogLoader.loadValidated(
        EquipmentCatalogVersion.SCALING_V3
    );

    @Test
    void thirdCatalogRevisionChangesOnlyTheBowPrimaryAttack() {
        Assertions.assertEquals(V2.modifiers(), V3.modifiers());
        Assertions.assertEquals(V2.defaultItems(), V3.defaultItems());

        final var v2Objects = objectsByCode(V2);
        final var v3Objects = objectsByCode(V3);
        Assertions.assertEquals(v2Objects.keySet(), v3Objects.keySet());
        for (final var entry : v2Objects.entrySet()) {
            if (!entry.getKey().equals("bow")) {
                Assertions.assertEquals(entry.getValue(), v3Objects.get(entry.getKey()), entry.getKey());
            }
        }

        final var v2Bow = v2Objects.get("bow");
        final var v3Bow = v3Objects.get("bow");
        Assertions.assertEquals(
            List.of(
                new ItemAttack(AttackType.PIERCE, 1, 3, 300),
                new ItemAttack(AttackType.PIERCE, 3, 3, 120)
            ),
            v2Bow.attacks()
        );
        final var v3Attacks = List.of(
            new ItemAttack(AttackType.PIERCE, 1, 3, 240),
            new ItemAttack(AttackType.PIERCE, 3, 3, 120)
        );
        Assertions.assertEquals(v3Attacks, v3Bow.attacks());
        Assertions.assertEquals(copyWithAttacks(v2Bow, v3Attacks), v3Bow);
        Assertions.assertEquals(
            V2.itemObjects().stream()
                .map(item -> item.code().equals("bow") ? copyWithAttacks(item, v3Attacks) : item)
                .toList(),
            V3.itemObjects()
        );
    }

    @Test
    void revisedBowPrimaryAttackUsesExactVersionOneProgression() {
        final var v2Bow = objectsByCode(V2).get("bow");
        final var v3Bow = objectsByCode(V3).get("bow");

        for (int level = 0; level <= 21; level++) {
            final int currentLevel = level;
            final var v2State = ItemProgression.state(v2Bow, level);
            final var v3State = ItemProgression.state(v3Bow, level);
            Assertions.assertAll(
                "bow +" + level,
                () -> Assertions.assertEquals(300 + 5 * currentLevel, v2State.attacks().getFirst().attack()),
                () -> Assertions.assertEquals(240 + 4 * currentLevel, v3State.attacks().getFirst().attack()),
                () -> Assertions.assertEquals(v2State.attacks().get(1), v3State.attacks().get(1))
            );
        }
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
