package ru.homyakin.seeker.game.battle.skill.scaling;

import java.math.BigInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class NonNegativeRationalTest {
    @Test
    void normalizesAndCalculatesWithoutLosingPrecision() {
        final var first = NonNegativeRational.of(2, 4);
        final var second = NonNegativeRational.of(1, 3);

        Assertions.assertEquals(NonNegativeRational.of(1, 2), first);
        Assertions.assertEquals(NonNegativeRational.of(5, 6), first.add(second));
        Assertions.assertEquals(NonNegativeRational.of(1, 6), first.multiply(second));
        Assertions.assertEquals(NonNegativeRational.of(3, 2), first.divide(second));
    }

    @Test
    void supportsValuesLargerThanLong() {
        final var huge = BigInteger.TEN.pow(100);
        final var value = new NonNegativeRational(huge, BigInteger.valueOf(3));

        Assertions.assertEquals(huge.multiply(BigInteger.TWO), value.multiply(6).integerValueExact());
    }

    @Test
    void exposesExactFloorAndHalfUpRounding() {
        Assertions.assertEquals(BigInteger.ONE, NonNegativeRational.of(3, 2).floor());
        Assertions.assertEquals(BigInteger.TWO, NonNegativeRational.of(3, 2).roundHalfUp());
        Assertions.assertEquals(BigInteger.ONE, NonNegativeRational.of(149, 100).roundHalfUp());
    }

    @Test
    void rejectsNegativeValuesAndInvalidOperations() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> NonNegativeRational.of(-1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> NonNegativeRational.of(1, 0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> NonNegativeRational.ONE.multiply(-1));
        Assertions.assertThrows(ArithmeticException.class, () -> NonNegativeRational.ONE.divide(
            NonNegativeRational.ZERO
        ));
        Assertions.assertThrows(ArithmeticException.class, () -> NonNegativeRational.of(1, 2).integerValueExact());
    }
}
