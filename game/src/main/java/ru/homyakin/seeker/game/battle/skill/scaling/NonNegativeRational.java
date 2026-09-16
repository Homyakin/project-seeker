package ru.homyakin.seeker.game.battle.skill.scaling;

import java.math.BigInteger;
import java.util.Objects;

/**
 * An exact, normalized non-negative rational number.
 */
public record NonNegativeRational(BigInteger numerator, BigInteger denominator)
    implements Comparable<NonNegativeRational> {
    public static final NonNegativeRational ZERO = of(0);
    public static final NonNegativeRational ONE = of(1);

    public NonNegativeRational {
        Objects.requireNonNull(numerator, "numerator");
        Objects.requireNonNull(denominator, "denominator");
        if (numerator.signum() < 0) {
            throw new IllegalArgumentException("Numerator must be non-negative: " + numerator);
        }
        if (denominator.signum() <= 0) {
            throw new IllegalArgumentException("Denominator must be positive: " + denominator);
        }
        if (numerator.signum() == 0) {
            denominator = BigInteger.ONE;
        } else {
            final var divisor = numerator.gcd(denominator);
            numerator = numerator.divide(divisor);
            denominator = denominator.divide(divisor);
        }
    }

    public static NonNegativeRational of(long value) {
        return new NonNegativeRational(BigInteger.valueOf(value), BigInteger.ONE);
    }

    public static NonNegativeRational of(long numerator, long denominator) {
        return new NonNegativeRational(BigInteger.valueOf(numerator), BigInteger.valueOf(denominator));
    }

    public NonNegativeRational add(NonNegativeRational other) {
        Objects.requireNonNull(other, "other");
        return new NonNegativeRational(
            numerator.multiply(other.denominator).add(other.numerator.multiply(denominator)),
            denominator.multiply(other.denominator)
        );
    }

    public NonNegativeRational multiply(NonNegativeRational other) {
        Objects.requireNonNull(other, "other");
        return new NonNegativeRational(
            numerator.multiply(other.numerator),
            denominator.multiply(other.denominator)
        );
    }

    public NonNegativeRational multiply(long multiplier) {
        return multiply(BigInteger.valueOf(multiplier));
    }

    public NonNegativeRational multiply(BigInteger multiplier) {
        Objects.requireNonNull(multiplier, "multiplier");
        if (multiplier.signum() < 0) {
            throw new IllegalArgumentException("Multiplier must be non-negative: " + multiplier);
        }
        return new NonNegativeRational(numerator.multiply(multiplier), denominator);
    }

    public NonNegativeRational divide(NonNegativeRational divisor) {
        Objects.requireNonNull(divisor, "divisor");
        if (divisor.isZero()) {
            throw new ArithmeticException("Division by zero");
        }
        return new NonNegativeRational(
            numerator.multiply(divisor.denominator),
            denominator.multiply(divisor.numerator)
        );
    }

    public BigInteger floor() {
        return numerator.divide(denominator);
    }

    public BigInteger roundHalfUp() {
        return numerator.multiply(BigInteger.TWO).add(denominator)
            .divide(denominator.multiply(BigInteger.TWO));
    }

    public BigInteger integerValueExact() {
        final var result = numerator.divideAndRemainder(denominator);
        if (result[1].signum() != 0) {
            throw new ArithmeticException("Rational value is not an integer: " + this);
        }
        return result[0];
    }

    public boolean isZero() {
        return numerator.signum() == 0;
    }

    public double doubleValue() {
        return numerator.doubleValue() / denominator.doubleValue();
    }

    @Override
    public int compareTo(NonNegativeRational other) {
        Objects.requireNonNull(other, "other");
        return numerator.multiply(other.denominator).compareTo(other.numerator.multiply(denominator));
    }
}
