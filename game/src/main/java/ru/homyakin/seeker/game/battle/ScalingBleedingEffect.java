package ru.homyakin.seeker.game.battle;

import java.util.Map;
import java.util.Objects;
import ru.homyakin.seeker.game.item.models.AttackType;

/** Remembered scalable bleeding from one source. */
final class ScalingBleedingEffect {
    private final BattlePersonage source;
    private final Map<AttackType, Integer> basis;
    private final int coefficientNumerator;
    private final int coefficientDenominator;
    private int ticksRemaining;

    ScalingBleedingEffect(
        BattlePersonage source,
        Map<AttackType, Integer> basis,
        int coefficientNumerator,
        int coefficientDenominator,
        int ticksRemaining
    ) {
        this.source = Objects.requireNonNull(source, "source");
        this.basis = Map.copyOf(basis);
        if (basis.isEmpty() || basis.values().stream().anyMatch(value -> value == null || value <= 0)) {
            throw new IllegalArgumentException("bleeding basis must contain positive values");
        }
        if (coefficientNumerator <= 0 || coefficientDenominator <= 0) {
            throw new IllegalArgumentException("bleeding coefficient must be positive");
        }
        if (ticksRemaining <= 0) {
            throw new IllegalArgumentException("bleeding ticks must be positive");
        }
        this.coefficientNumerator = coefficientNumerator;
        this.coefficientDenominator = coefficientDenominator;
        this.ticksRemaining = ticksRemaining;
    }

    BattlePersonage source() {
        return source;
    }

    Map<AttackType, Integer> basis() {
        return basis;
    }

    int coefficientNumerator() {
        return coefficientNumerator;
    }

    int coefficientDenominator() {
        return coefficientDenominator;
    }

    boolean consumeTick() {
        ticksRemaining--;
        return ticksRemaining == 0;
    }
}
