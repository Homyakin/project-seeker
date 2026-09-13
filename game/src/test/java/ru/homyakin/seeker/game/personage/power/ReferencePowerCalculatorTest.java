package ru.homyakin.seeker.game.personage.power;

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

class ReferencePowerCalculatorTest {
    private static final double TOLERANCE = 1e-9;

    @Test
    void unscaledPowerIsSurvivabilityTimesNormalDamagePerTime() {
        final var items = List.of(item(
            List.of(new ItemAttack(AttackType.SLASH, 1, 1, 75)),
            Optional.empty(),
            120,
            20,
            25,
            0.8,
            500,
            0,
            0,
            Optional.empty(),
            ItemRarity.COMMON
        ));

        final var power = ReferencePowerCalculator.calculate(items, 0.25);

        Assertions.assertAll(
            () -> Assertions.assertEquals(160, power.survivability(), TOLERANCE),
            () -> Assertions.assertEquals(45, power.normalDamagePerTime(), TOLERANCE),
            () -> Assertions.assertEquals(7_200, power.unscaled(), TOLERANCE),
            () -> Assertions.assertEquals(
                power.survivability() * power.normalDamagePerTime(),
                power.unscaled(),
                TOLERANCE
            ),
            () -> Assertions.assertEquals(1_800, power.displayed(), TOLERANCE)
        );
    }

    @Test
    void workingAttackUsesBestSumAvailableAtOneRange() {
        final var items = List.of(item(
            List.of(
                new ItemAttack(AttackType.SLASH, 1, 1, 80),
                new ItemAttack(AttackType.MAGICAL, 2, 2, 120),
                new ItemAttack(AttackType.PIERCE, 1, 2, 30)
            ),
            Optional.empty(),
            100,
            0,
            0,
            0,
            1_000,
            0,
            0,
            Optional.empty(),
            ItemRarity.COMMON
        ));

        final var power = ReferencePowerCalculator.calculate(items, 1);

        Assertions.assertEquals(150, power.normalDamagePerTime(), TOLERANCE);
        Assertions.assertEquals(15_000, power.unscaled(), TOLERANCE);
    }

    @Test
    void defenseUsesTheNeutralAverageAcrossEveryAttackType() {
        final var items = List.of(item(
            List.of(new ItemAttack(AttackType.SLASH, 1, 1, 10)),
            Optional.of(new ItemDefense(DefenseType.CLOTH, 100)),
            100,
            0,
            0,
            0,
            1_000,
            0,
            0,
            Optional.empty(),
            ItemRarity.COMMON
        ));
        final var averageDamageTaken = (
            500.0 / 575
                + 500.0 / 625
                + 500.0 / 590
                + 500.0 / 610
        ) / 4;

        final var power = ReferencePowerCalculator.calculate(items, 1);

        Assertions.assertEquals(100 / averageDamageTaken, power.survivability(), TOLERANCE);
        Assertions.assertEquals(power.survivability() * 10, power.unscaled(), TOLERANCE);
    }

    @Test
    void speedIsLinearUntilInitiativeThresholdAndThenCapped() {
        final var slow = ReferencePowerCalculator.calculate(
            List.of(basicItem(100, 100, 400)),
            1
        );
        final var threshold = ReferencePowerCalculator.calculate(
            List.of(basicItem(100, 100, 1_000)),
            1
        );
        final var fasterThanThreshold = ReferencePowerCalculator.calculate(
            List.of(basicItem(100, 100, 1_500)),
            1
        );

        Assertions.assertAll(
            () -> Assertions.assertEquals(40, slow.normalDamagePerTime(), TOLERANCE),
            () -> Assertions.assertEquals(100, threshold.normalDamagePerTime(), TOLERANCE),
            () -> Assertions.assertEquals(
                threshold.normalDamagePerTime(),
                fasterThanThreshold.normalDamagePerTime(),
                TOLERANCE
            )
        );
    }

    @Test
    void zeroHealthAttackOrSpeedProducesZeroPower() {
        final var zeroHealth = basicItem(0, 100, 1_000);
        final var noAttack = item(
            List.of(),
            Optional.empty(),
            100,
            0,
            0,
            0,
            1_000,
            0,
            0,
            Optional.empty(),
            ItemRarity.COMMON
        );
        final var zeroSpeed = basicItem(100, 100, 0);

        Assertions.assertAll(
            () -> Assertions.assertEquals(0,
                ReferencePowerCalculator.calculate(List.of(zeroHealth), 1).unscaled(), TOLERANCE),
            () -> Assertions.assertEquals(0,
                ReferencePowerCalculator.calculate(List.of(noAttack), 1).unscaled(), TOLERANCE),
            () -> Assertions.assertEquals(0,
                ReferencePowerCalculator.calculate(List.of(zeroSpeed), 1).unscaled(), TOLERANCE)
        );
    }

    @Test
    void displayScaleChangesOnlyDisplayedValue() {
        final var items = List.of(basicItem(100, 100, 1_000));

        final var smallScale = ReferencePowerCalculator.calculate(items, 0.25);
        final var largeScale = ReferencePowerCalculator.calculate(items, 2);

        Assertions.assertAll(
            () -> Assertions.assertEquals(smallScale.survivability(), largeScale.survivability()),
            () -> Assertions.assertEquals(smallScale.normalDamagePerTime(), largeScale.normalDamagePerTime()),
            () -> Assertions.assertEquals(smallScale.unscaled(), largeScale.unscaled()),
            () -> Assertions.assertEquals(smallScale.displayed() * 8, largeScale.displayed(), TOLERANCE)
        );
    }

    @Test
    void threatImpactRarityAndModifierDoNotInfluencePower() {
        final var base = item(
            List.of(new ItemAttack(AttackType.BLUNT, 1, 2, 100)),
            Optional.of(new ItemDefense(DefenseType.PLATE, 80)),
            150,
            10,
            5,
            0.3,
            600,
            0,
            0,
            Optional.empty(),
            ItemRarity.COMMON
        );
        final var unrelatedFieldsChanged = item(
            List.of(new ItemAttack(AttackType.BLUNT, 1, 2, 100)),
            Optional.of(new ItemDefense(DefenseType.PLATE, 80)),
            150,
            10,
            5,
            0.3,
            600,
            999,
            20,
            Optional.of(new Modifier(ActiveEnum.BERSERK)),
            ItemRarity.LEGENDARY
        );

        Assertions.assertEquals(
            ReferencePowerCalculator.calculate(List.of(base), 1),
            ReferencePowerCalculator.calculate(List.of(unrelatedFieldsChanged), 1)
        );
    }

    @Test
    void invalidInputsAndOverflowFailExplicitly() {
        final var negativeHealth = basicItem(-1, 100, 1_000);
        final var guaranteedDodge = item(
            List.of(new ItemAttack(AttackType.SLASH, 1, 1, 100)),
            Optional.empty(),
            100,
            0,
            100,
            0,
            1_000,
            0,
            0,
            Optional.empty(),
            ItemRarity.COMMON
        );
        final var maximumHealth = basicItem(Integer.MAX_VALUE, 100, 1_000);

        Assertions.assertAll(
            () -> Assertions.assertThrows(IllegalArgumentException.class,
                () -> ReferencePowerCalculator.calculate(null, 1)),
            () -> Assertions.assertThrows(IllegalArgumentException.class,
                () -> ReferencePowerCalculator.calculate(List.of(negativeHealth), 1)),
            () -> Assertions.assertThrows(IllegalArgumentException.class,
                () -> ReferencePowerCalculator.calculate(List.of(guaranteedDodge), 1)),
            () -> Assertions.assertThrows(IllegalArgumentException.class,
                () -> ReferencePowerCalculator.calculate(List.of(maximumHealth), 0)),
            () -> Assertions.assertThrows(ArithmeticException.class,
                () -> ReferencePowerCalculator.calculate(List.of(maximumHealth, maximumHealth), 1))
        );
    }

    private static Item basicItem(int health, int attack, int speed) {
        final var attacks = attack == 0
            ? List.<ItemAttack>of()
            : List.of(new ItemAttack(AttackType.SLASH, 1, 1, attack));
        return item(
            attacks,
            Optional.empty(),
            health,
            0,
            0,
            0,
            speed,
            0,
            0,
            Optional.empty(),
            ItemRarity.COMMON
        );
    }

    private static Item item(
        List<ItemAttack> attacks,
        Optional<ItemDefense> defense,
        int health,
        int critChance,
        int dodgeChance,
        double critMultiplier,
        int speed,
        int baseThreat,
        int impact,
        Optional<Modifier> modifier,
        ItemRarity rarity
    ) {
        final var object = new ItemObject(
            "power-test",
            Set.of(PersonageSlot.MAIN_HAND),
            attacks,
            defense,
            health,
            critChance,
            dodgeChance,
            critMultiplier,
            speed,
            baseThreat,
            impact,
            ItemProgressionVersion.V1,
            Map.of()
        );
        return new Item(object, modifier, rarity, 0);
    }
}
