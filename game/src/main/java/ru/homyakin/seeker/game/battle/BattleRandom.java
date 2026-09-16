package ru.homyakin.seeker.game.battle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Random values split into named sequences so an unrelated roll does not shift another combat decision.
 */
public interface BattleRandom {
    int nextInt(String sequence, int minimumInclusive, int maximumInclusive);

    default boolean chance(String sequence, int basisPoints) {
        if (basisPoints <= 0) {
            return false;
        }
        if (basisPoints >= 10_000) {
            return true;
        }
        return nextInt(sequence, 1, 10_000) <= basisPoints;
    }

    default <T> T pickWeighted(String sequence, Map<T, Integer> weights) {
        if (weights.isEmpty()) {
            throw new IllegalArgumentException("weights must not be empty");
        }
        final var stableWeights = new LinkedHashMap<T, Integer>();
        var total = 0L;
        for (final var entry : weights.entrySet()) {
            if (entry.getValue() <= 0) {
                throw new IllegalArgumentException("weight must be positive: " + entry.getValue());
            }
            total = Math.addExact(total, entry.getValue());
            if (total > Integer.MAX_VALUE) {
                throw new ArithmeticException("total weight exceeds integer range");
            }
            stableWeights.put(entry.getKey(), entry.getValue());
        }
        var roll = nextInt(sequence, 1, (int) total);
        for (final var entry : stableWeights.entrySet()) {
            roll -= entry.getValue();
            if (roll <= 0) {
                return entry.getKey();
            }
        }
        throw new IllegalStateException("weighted selection did not produce a value");
    }

    default <T> List<T> shuffle(String sequence, List<T> values) {
        final var shuffled = new ArrayList<>(values);
        for (int i = shuffled.size() - 1; i > 0; i--) {
            final int replacement = nextInt(sequence, 0, i);
            final var value = shuffled.get(i);
            shuffled.set(i, shuffled.get(replacement));
            shuffled.set(replacement, value);
        }
        return shuffled;
    }
}
