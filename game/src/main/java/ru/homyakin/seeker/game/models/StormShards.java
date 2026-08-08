package ru.homyakin.seeker.game.models;

import com.fasterxml.jackson.annotation.JsonCreator;

public record StormShards(
    int value
) implements Comparable<StormShards> {

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static StormShards from(int value) {
        return new StormShards(value);
    }

    public static StormShards zero() {
        return ZERO;
    }

    public StormShards add(StormShards other) {
        int increase = other.value;
        if (increase > 0) {
            if (Integer.MAX_VALUE - increase < value) {
                return new StormShards(Integer.MAX_VALUE);
            }
        } else if (increase < 0) {
            if (-increase > value) {
                throw new IllegalStateException(
                    "StormShards can't be less than value: %d > %d".formatted(increase, value)
                );
            }
        }
        return new StormShards(this.value + increase);
    }

    public boolean lessThan(StormShards other) {
        return this.value < other.value;
    }

    public StormShards negative() {
        return new StormShards(-value);
    }

    public boolean isZero() {
        return value == 0;
    }

    @Override
    public int compareTo(StormShards other) {
        return Integer.compare(value, other.value);
    }

    public static final StormShards ZERO = new StormShards(0);
}
