package ru.homyakin.seeker.game.personage.models;

public record Money(
    int value
) {
    public static Money zero() {
        return new Money(0);
    }

    public Money add(int increase) {
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
}
