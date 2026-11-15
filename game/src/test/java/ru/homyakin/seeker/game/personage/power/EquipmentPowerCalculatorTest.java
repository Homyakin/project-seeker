package ru.homyakin.seeker.game.personage.power;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import ru.homyakin.seeker.game.battle.CombatRules;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogLoader;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogVersion;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.DefenseType;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemAttack;
import ru.homyakin.seeker.game.item.models.ItemDefense;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.ItemProgressionVersion;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;

class EquipmentPowerCalculatorTest {
    @ParameterizedTest
    @EnumSource(value = EquipmentCatalogVersion.class, names = {"SCALING_V3", "SCALING_V4", "SCALING_V5"})
    void catalogAndEnhancedKitsRetainPreviousArithmeticBitForBit(EquipmentCatalogVersion version) {
        final var objects = EquipmentCatalogLoader.loadValidated(version).itemObjects().stream()
            .collect(Collectors.toMap(ItemObject::code, object -> object));
        for (final var level : List.of(0, 1, 10, 60)) {
            final var kit = PowerDisplayVersion.REFERENCE_V1_ITEM_CODES.stream()
                .map(code -> new Item(objects.get(code), Optional.empty(), ItemRarity.COMMON, level))
                .toList();
            assertPreviousArithmetic(kit);
        }
        for (final var object : objects.values()) {
            assertPreviousArithmetic(List.of(new Item(object, Optional.empty(), ItemRarity.COMMON, 7)));
        }
    }

    @Test
    void frozenReferenceAndSimpleUnarmoredGoldenRemainUnchanged() {
        final var reference = PowerDisplayServiceTest.referenceItems();
        final var power = EquipmentPowerCalculator.calculate(reference);
        // Captured from the unchanged pre-extraction calculator; the old decimal assertion used a tolerance.
        Assertions.assertEquals(
            Double.doubleToLongBits(0x1.2689baf62a341p18),
            Double.doubleToLongBits(power.unscaled())
        );
        Assertions.assertEquals(1_000, PowerDisplayVersion.REFERENCE_V1.referenceValue(reference));
        final var ordinary = EquipmentPowerCalculator.calculate(List.of(item(
            120, 500, 20, 25, 0.8,
            List.of(new ItemAttack(AttackType.SLASH, 1, 1, 75)), Optional.empty()
        )));
        Assertions.assertEquals(new EquipmentPower(160, 45, 7_200), ordinary);
    }

    @Test
    void mixedDefensesRangesDodgeCritAndSpeedUseThePreviousOperatorOrder() {
        final var items = List.of(
            item(253, 713, 17, 13, 0.35, List.of(
                new ItemAttack(AttackType.SLASH, 1, 1, 137),
                new ItemAttack(AttackType.MAGICAL, 3, 6, 271)
            ), Optional.of(new ItemDefense(DefenseType.CLOTH, 317))),
            item(479, 587, 23, 19, 0.55, List.of(
                new ItemAttack(AttackType.BLUNT, 2, 4, 89),
                new ItemAttack(AttackType.PIERCE, 4, Integer.MAX_VALUE, 71)
            ), Optional.of(new ItemDefense(DefenseType.PLATE, 193)))
        );
        assertPreviousArithmetic(items);
        final var atThreshold = List.of(item(
            100, 1_000, 100, 99, 0.8,
            List.of(new ItemAttack(AttackType.PIERCE, 1, 1, 100)), Optional.empty()
        ));
        assertPreviousArithmetic(atThreshold);
        final var capped = EquipmentPowerCalculator.calculate(atThreshold);
        Assertions.assertEquals(200, capped.normalDamagePerTime());
        Assertions.assertEquals(100 / (1 - 99 / 100.0), capped.survivability());
    }

    @Test
    void emptyAndZeroGrowthChannelsRemainValid() {
        Assertions.assertEquals(new EquipmentPower(0, 0, 0), EquipmentPowerCalculator.calculate(List.of()));
        assertPreviousArithmetic(List.of(item(0, 0, 0, 0, 0, List.of(), Optional.empty())));
        assertPreviousArithmetic(List.of(item(
            100, 0, 0, 0, 0,
            List.of(new ItemAttack(AttackType.SLASH, 1, 1, 200)), Optional.empty()
        )));
    }

    @Test
    void invalidProbabilitiesNonFiniteInputsAndOverflowAreNotSilentlyClamped() {
        final var invalidNull = new ArrayList<Item>();
        invalidNull.add(null);
        Assertions.assertThrows(IllegalArgumentException.class, () -> EquipmentPowerCalculator.calculate(null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> EquipmentPowerCalculator.calculate(invalidNull));
        for (final var invalid : List.of(
            item(100, 100, 101, 0, 0, List.of(), Optional.empty()),
            item(100, 100, 0, 100, 0, List.of(), Optional.empty()),
            item(100, 100, -1, 0, 0, List.of(), Optional.empty()),
            item(100, 100, 0, -1, 0, List.of(), Optional.empty()),
            item(100, 100, 0, 0, Double.NaN, List.of(), Optional.empty()),
            item(100, 100, 0, 0, Double.POSITIVE_INFINITY, List.of(), Optional.empty()),
            item(100, 100, 0, 0, -0.1, List.of(), Optional.empty())
        )) {
            Assertions.assertThrows(IllegalArgumentException.class,
                () -> EquipmentPowerCalculator.calculate(List.of(invalid)));
        }
        final var maximum = item(Integer.MAX_VALUE, 100, 0, 0, 0, List.of(), Optional.empty());
        Assertions.assertThrows(ArithmeticException.class,
            () -> EquipmentPowerCalculator.calculate(List.of(maximum, maximum)));
        final var finiteInputWithInfiniteResult = item(
            100, 1_000, 100, 0, Double.MAX_VALUE,
            List.of(new ItemAttack(AttackType.SLASH, 1, 1, 2)), Optional.empty()
        );
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> EquipmentPowerCalculator.calculate(List.of(finiteInputWithInfiniteResult)));
    }

    private static void assertPreviousArithmetic(List<Item> items) {
        final var expected = previousArithmetic(items);
        final var actual = EquipmentPowerCalculator.calculate(items);
        final var displayed = ReferencePowerCalculator.calculate(items, 0.123456789);
        Assertions.assertAll(
            () -> Assertions.assertEquals(expected, actual),
            () -> Assertions.assertEquals(expected.survivability(), displayed.survivability()),
            () -> Assertions.assertEquals(expected.normalDamagePerTime(), displayed.normalDamagePerTime()),
            () -> Assertions.assertEquals(expected.unscaled(), displayed.unscaled()),
            () -> Assertions.assertEquals(expected.unscaled() * 0.123456789, displayed.displayed())
        );
    }

    /** Independent range enumeration and the pre-extraction arithmetic, not a call through the display wrapper. */
    private static EquipmentPower previousArithmetic(List<Item> items) {
        var health = 0;
        var speed = 0;
        var critChance = 0;
        var dodgeChance = 0;
        var critMultiplier = 1.2;
        final Map<DefenseType, Integer> defenses = new EnumMap<>(DefenseType.class);
        final var attacks = new ArrayList<ItemAttack>();
        for (final var item : items) {
            health = Math.addExact(health, item.health());
            speed = Math.addExact(speed, item.speed());
            critChance = Math.addExact(critChance, item.critChance());
            dodgeChance = Math.addExact(dodgeChance, item.dodgeChance());
            critMultiplier += item.critMultiplier();
            item.itemDefense().ifPresent(defense -> defenses.merge(
                defense.defenseType(), defense.defense(), Math::addExact
            ));
            attacks.addAll(item.itemAttacks());
        }
        var workingAttack = 0;
        for (final var boundary : attacks.stream().mapToInt(ItemAttack::minRange).distinct().toArray()) {
            var atBoundary = 0;
            for (final var attack : attacks) {
                if (attack.isAvailableAt(boundary)) {
                    atBoundary = Math.addExact(atBoundary, attack.attack());
                }
            }
            workingAttack = Math.max(workingAttack, atBoundary);
        }
        final var averageDamageTaken = Arrays.stream(AttackType.values())
            .mapToDouble(type -> CombatRules.damageTakenMultiplier(defenses, type)).average().orElseThrow();
        final var dodgeMultiplier = 1 - dodgeChance / 100.0;
        final var survivability = health == 0 ? 0 : health / (dodgeMultiplier * averageDamageTaken);
        final var critFactor = 1 + critChance / 100.0 * (critMultiplier - 1);
        final var damage = (double) Math.min(speed, 1_000) / 1_000 * critFactor * workingAttack;
        return new EquipmentPower(survivability, damage, survivability * damage);
    }

    private static Item item(
        int health,
        int speed,
        int critChance,
        int dodgeChance,
        double critMultiplier,
        List<ItemAttack> attacks,
        Optional<ItemDefense> defense
    ) {
        return new Item(new ItemObject(
            "equipment-power-test", Set.of(PersonageSlot.MAIN_HAND), attacks, defense,
            health, critChance, dodgeChance, critMultiplier, speed, 0, 0, ItemProgressionVersion.V1, Map.of()
        ), Optional.empty(), ItemRarity.COMMON, 0);
    }
}
