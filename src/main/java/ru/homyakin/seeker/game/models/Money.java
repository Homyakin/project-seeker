package ru.homyakin.seeker.game.models;

import com.fasterxml.jackson.annotation.JsonCreator;

public record Money(
    int value
) implements Comparable<Money> {

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Money from(int value) {
        return new Money(value);
    }

    public static Money zero() {
        return ZERO;
    }

    public Money add(Money money) {
        int increase = money.value;
        if (increase > 0) {
            if (Integer.MAX_VALUE - increase < value) {
                return new Money(Integer.MAX_VALUE);
            }
        } else if (increase < 0) {
            if (-increase > value) {
                throw new IllegalStateException(
                    "Money can't be less than value: %d > %d".formatted(increase, value)
                );
            }
        }
        return new Money(this.value + increase);
    }

    public boolean lessThan(Money other) {
        return this.value < other.value;
    }

    public Money negative() {
        return new Money(-value);
    }

    public Money divide(int divider) {
        return new Money(value / divider);
    }

    public boolean isNegative() {
        return value < 0;
    }

    public boolean isZero() {
        return value == 0;
    }

    @Override
    public int compareTo(Money other) {
        return Integer.compare(value, other.value);
    }

    private static final Money ZERO = new Money(0);
}
