package ru.homyakin.seeker.game.battle.skill.scaling;

import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.battle.CombatRules;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.DefenseType;

class SkillDamageCalculatorTest {
    @Test
    void exposesExactDefenseWeightsWithoutChangingDoubleApi() {
        Assertions.assertEquals(
            NonNegativeRational.of(3, 4),
            CombatRules.exactDefenseWeight(DefenseType.CLOTH, AttackType.SLASH)
        );
        Assertions.assertEquals(
            NonNegativeRational.of(5, 4),
            CombatRules.exactDefenseWeight(DefenseType.CLOTH, AttackType.BLUNT)
        );
        Assertions.assertEquals(0.75, CombatRules.defenseWeight(DefenseType.CLOTH, AttackType.SLASH));
    }

    @Test
    void floorsMixedDamageOnceAfterAllDefenseComponents() {
        final var result = SkillDamageCalculator.calculate(
            Map.of(
                AttackType.SLASH, NonNegativeRational.ONE,
                AttackType.BLUNT, NonNegativeRational.ONE
            ),
            Map.of(DefenseType.CLOTH, 500),
            (minimum, maximum) -> 0
        );

        Assertions.assertEquals(NonNegativeRational.of(64, 63), result.exactDamageAfterDefense());
        Assertions.assertEquals(1, result.damageAfterDefense());
        Assertions.assertEquals(1, result.damage());
        Assertions.assertEquals(2, result.rawDamage().size());
    }

    @Test
    void floorsBeforeApplyingOneSymmetricIntegerSpread() {
        final var result = SkillDamageCalculator.calculate(
            Map.of(AttackType.MAGICAL, NonNegativeRational.of(21, 2)),
            Map.of(),
            (minimum, maximum) -> {
                Assertions.assertEquals(-1, minimum);
                Assertions.assertEquals(1, maximum);
                return maximum;
            }
        );

        Assertions.assertEquals(NonNegativeRational.of(21, 2), result.exactDamageAfterDefense());
        Assertions.assertEquals(10, result.damageAfterDefense());
        Assertions.assertEquals(1, result.spreadBound());
        Assertions.assertEquals(1, result.spreadDeviation());
        Assertions.assertEquals(11, result.damage());
    }

    @Test
    void zeroRawDamageProducesZeroWithoutConsumingSpread() {
        final var called = new AtomicBoolean();

        final var result = SkillDamageCalculator.calculate(
            Map.of(AttackType.SLASH, NonNegativeRational.ZERO),
            Map.of(),
            (minimum, maximum) -> {
                called.set(true);
                return 0;
            }
        );

        Assertions.assertEquals(0, result.damage());
        Assertions.assertTrue(result.rawDamage().isEmpty());
        Assertions.assertFalse(called.get());
    }

    @Test
    void positiveRawDamageCannotBecomeZeroAfterDefense() {
        final var result = SkillDamageCalculator.calculate(
            Map.of(AttackType.PIERCE, NonNegativeRational.of(1, 100)),
            Map.of(DefenseType.PLATE, Integer.MAX_VALUE),
            (minimum, maximum) -> 0
        );

        Assertions.assertEquals(1, result.damageAfterDefense());
        Assertions.assertEquals(1, result.damage());
    }

    @Test
    void rejectsSpreadOutsideInclusiveBounds() {
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> SkillDamageCalculator.calculate(
                Map.of(AttackType.SLASH, NonNegativeRational.of(100)),
                Map.of(),
                (minimum, maximum) -> maximum + 1
            )
        );
    }

    @Test
    void rejectsNegativeDefenseAndFinalIntegerOverflow() {
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> SkillDamageCalculator.calculate(
                Map.of(AttackType.SLASH, NonNegativeRational.ONE),
                Map.of(DefenseType.CLOTH, -1),
                (minimum, maximum) -> 0
            )
        );
        final var tooLarge = new NonNegativeRational(
            BigInteger.valueOf(Integer.MAX_VALUE).add(BigInteger.ONE),
            BigInteger.ONE
        );
        Assertions.assertThrows(
            ArithmeticException.class,
            () -> SkillDamageCalculator.calculate(
                Map.of(AttackType.SLASH, tooLarge),
                Map.of(),
                (minimum, maximum) -> 0
            )
        );
        Assertions.assertThrows(
            ArithmeticException.class,
            () -> SkillDamageCalculator.calculate(
                Map.of(AttackType.SLASH, NonNegativeRational.of(Integer.MAX_VALUE)),
                Map.of(),
                (minimum, maximum) -> maximum
            )
        );
    }
}
