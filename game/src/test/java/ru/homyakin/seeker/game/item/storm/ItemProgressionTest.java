package ru.homyakin.seeker.game.item.storm;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.DefenseType;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemAttack;
import ru.homyakin.seeker.game.item.models.ItemDefense;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.ItemProgressionVersion;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.item.models.Modifier;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;

class ItemProgressionTest {

    @Test
    void valueAtLevelCoversZeroLegacyAndNewCatalogBoundaries() {
        Assertions.assertAll(
            () -> Assertions.assertEquals(0, ItemProgression.valueAtLevel(0, 0)),
            () -> Assertions.assertEquals(0, ItemProgression.valueAtLevel(0, Integer.MAX_VALUE)),
            () -> Assertions.assertEquals(59, ItemProgression.valueAtLevel(59, 0)),
            () -> Assertions.assertEquals(117, ItemProgression.valueAtLevel(59, 59)),
            () -> Assertions.assertEquals(118, ItemProgression.valueAtLevel(59, 60)),
            () -> Assertions.assertEquals(60, ItemProgression.valueAtLevel(60, 0)),
            () -> Assertions.assertEquals(119, ItemProgression.valueAtLevel(60, 59)),
            () -> Assertions.assertEquals(120, ItemProgression.valueAtLevel(60, 60)),
            () -> Assertions.assertEquals(0, ItemProgression.transitionIncrease(59, 30)),
            () -> Assertions.assertEquals(1, ItemProgression.transitionIncrease(60, 30))
        );
    }

    @Test
    void valueAtLevelRoundsAccumulatedHalfUp() {
        Assertions.assertAll(
            () -> Assertions.assertEquals(3, ItemProgression.valueAtLevel(3, 9)),
            () -> Assertions.assertEquals(4, ItemProgression.valueAtLevel(3, 10)),
            () -> Assertions.assertEquals(4, ItemProgression.valueAtLevel(3, 11)),
            () -> Assertions.assertEquals(92, ItemProgression.valueAtLevel(90, 1))
        );
    }

    @Test
    void valueAtLevelIsCalculatedDirectlyFromBase() {
        Assertions.assertAll(
            () -> Assertions.assertEquals(61, ItemProgression.valueAtLevel(60, 1)),
            () -> Assertions.assertEquals(90, ItemProgression.valueAtLevel(60, 30)),
            () -> Assertions.assertEquals(120, ItemProgression.valueAtLevel(60, 60)),
            () -> Assertions.assertEquals(180, ItemProgression.valueAtLevel(60, 120)),
            () -> Assertions.assertEquals(
                ItemProgression.valueAtLevel(73, 17) + 73,
                ItemProgression.valueAtLevel(73, 77)
            )
        );
    }

    @Test
    void stateGrowsEveryAttackPartIndependentlyAndPreservesKeys() {
        final var object = object(
            List.of(
                new ItemAttack(AttackType.SLASH, 1, 1, 60),
                new ItemAttack(AttackType.MAGICAL, 2, 3, 90)
            ),
            Optional.of(new ItemDefense(DefenseType.ARCANE, 150)),
            90
        );

        final var state = ItemProgression.state(object, 1);

        Assertions.assertEquals(92, state.health());
        Assertions.assertEquals(
            List.of(
                new ItemAttack(AttackType.SLASH, 1, 1, 61),
                new ItemAttack(AttackType.MAGICAL, 2, 3, 92)
            ),
            state.attacks()
        );
        Assertions.assertEquals(
            Optional.of(new ItemDefense(DefenseType.ARCANE, 153)),
            state.defense()
        );
    }

    @Test
    void transitionIsTheDifferenceOfAdjacentStatesAndCanBeReversedExactly() {
        final var object = object(
            List.of(
                new ItemAttack(AttackType.PIERCE, 1, 2, 70),
                new ItemAttack(AttackType.BLUNT, 3, 3, 110)
            ),
            Optional.of(new ItemDefense(DefenseType.PLATE, 130)),
            170
        );
        final var current = ItemProgression.state(object, 7);
        final var next = ItemProgression.state(object, 8);
        final var delta = ItemProgression.transition(object, 7);

        Assertions.assertEquals(next.health(), current.health() + delta.health());
        Assertions.assertEquals(current.health(), next.health() - delta.health());
        for (int i = 0; i < current.attacks().size(); i++) {
            Assertions.assertEquals(
                next.attacks().get(i).attack(),
                current.attacks().get(i).attack() + delta.attacks().get(i).attack()
            );
            Assertions.assertEquals(
                current.attacks().get(i).attack(),
                next.attacks().get(i).attack() - delta.attacks().get(i).attack()
            );
        }
        Assertions.assertEquals(
            next.defense().orElseThrow().defense(),
            current.defense().orElseThrow().defense() + delta.defense().orElseThrow().defense()
        );
        Assertions.assertEquals(
            current.defense().orElseThrow().defense(),
            next.defense().orElseThrow().defense() - delta.defense().orElseThrow().defense()
        );
    }

    @Test
    void transitionIncreasesTelescopeToTheDirectState() {
        final var base = 73;
        final var startLevel = 11;
        final var endLevel = 84;
        var transitionSum = 0;
        for (int level = startLevel; level < endLevel; level++) {
            transitionSum += ItemProgression.transitionIncrease(base, level);
        }

        Assertions.assertEquals(
            ItemProgression.valueAtLevel(base, endLevel),
            ItemProgression.valueAtLevel(base, startLevel) + transitionSum
        );
    }

    @Test
    void enhancingItemLeavesSecondaryCharacteristicsAndInstanceDataUnchanged() {
        final var modifier = new Modifier(ActiveEnum.BERSERK);
        final var object = new ItemObject(
            "mixed",
            Set.of(PersonageSlot.MAIN_HAND, PersonageSlot.OFF_HAND),
            List.of(new ItemAttack(AttackType.SLASH, 2, 4, 90)),
            Optional.of(new ItemDefense(DefenseType.LEATHER, 120)),
            180,
            17,
            13,
            0.75,
            321,
            42,
            9,
            ItemProgressionVersion.V1,
            Map.of()
        );
        final var base = new Item(object, Optional.of(modifier), ItemRarity.LEGENDARY, 0);
        final var enhanced = new Item(object, Optional.of(modifier), ItemRarity.LEGENDARY, 17);

        Assertions.assertAll(
            () -> Assertions.assertEquals(base.critChance(), enhanced.critChance()),
            () -> Assertions.assertEquals(base.dodgeChance(), enhanced.dodgeChance()),
            () -> Assertions.assertEquals(base.critMultiplier(), enhanced.critMultiplier()),
            () -> Assertions.assertEquals(base.speed(), enhanced.speed()),
            () -> Assertions.assertEquals(base.baseThreat(), enhanced.baseThreat()),
            () -> Assertions.assertEquals(base.impact(), enhanced.impact()),
            () -> Assertions.assertEquals(base.rarity(), enhanced.rarity()),
            () -> Assertions.assertEquals(base.modifier(), enhanced.modifier()),
            () -> Assertions.assertEquals(base.object().slots(), enhanced.object().slots()),
            () -> Assertions.assertEquals(base.itemAttacks().getFirst().attackType(),
                enhanced.itemAttacks().getFirst().attackType()),
            () -> Assertions.assertEquals(base.itemAttacks().getFirst().minRange(),
                enhanced.itemAttacks().getFirst().minRange()),
            () -> Assertions.assertEquals(base.itemAttacks().getFirst().maxRange(),
                enhanced.itemAttacks().getFirst().maxRange())
        );
    }

    @Test
    void invalidInputsAndOverflowFailExplicitly() {
        final var noGrowthChannels = object(List.of(), Optional.empty(), 0);
        final var duplicateAttackParts = object(
            List.of(
                new ItemAttack(AttackType.SLASH, 1, 2, 60),
                new ItemAttack(AttackType.SLASH, 1, 2, 90)
            ),
            Optional.empty(),
            0
        );
        final var zeroDefense = object(
            List.of(),
            Optional.of(new ItemDefense(DefenseType.CLOTH, 0)),
            60
        );

        Assertions.assertAll(
            () -> Assertions.assertThrows(IllegalArgumentException.class,
                () -> ItemProgression.valueAtLevel(-1, 0)),
            () -> Assertions.assertThrows(IllegalArgumentException.class,
                () -> ItemProgression.valueAtLevel(1, -1)),
            () -> Assertions.assertThrows(IllegalArgumentException.class,
                () -> ItemProgression.state(null, 0)),
            () -> Assertions.assertThrows(IllegalArgumentException.class,
                () -> ItemProgression.state(noGrowthChannels, 0)),
            () -> Assertions.assertThrows(IllegalArgumentException.class,
                () -> ItemProgression.state(duplicateAttackParts, 0)),
            () -> Assertions.assertThrows(IllegalArgumentException.class,
                () -> ItemProgression.state(zeroDefense, 0)),
            () -> Assertions.assertThrows(ArithmeticException.class,
                () -> ItemProgression.valueAtLevel(Integer.MAX_VALUE, 1)),
            () -> Assertions.assertThrows(ArithmeticException.class,
                () -> ItemProgression.transitionIncrease(1, Integer.MAX_VALUE))
        );
    }

    @Test
    void maxSupportedLevelStopsBeforeOverflow() {
        final var object = object(List.of(), Optional.empty(), Integer.MAX_VALUE);

        Assertions.assertEquals(0, ItemProgression.maxSupportedLevel(object));
        Assertions.assertEquals(Integer.MAX_VALUE, ItemProgression.state(object, 0).health());
        Assertions.assertThrows(ArithmeticException.class, () -> ItemProgression.state(object, 1));
    }

    private static ItemObject object(
        List<ItemAttack> attacks,
        Optional<ItemDefense> defense,
        int health
    ) {
        return new ItemObject(
            "test",
            Set.of(PersonageSlot.MAIN_HAND),
            attacks,
            defense,
            health,
            0,
            0,
            0,
            0,
            0,
            0,
            ItemProgressionVersion.V1,
            Map.of()
        );
    }
}
