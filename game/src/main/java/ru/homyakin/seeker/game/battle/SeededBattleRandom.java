package ru.homyakin.seeker.game.battle;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Reproducible battle random whose named sequences are independent of one another.
 */
public final class SeededBattleRandom implements BattleRandom {
    private static final long FNV_OFFSET_BASIS = 0xcbf29ce484222325L;
    private static final long FNV_PRIME = 0x100000001b3L;

    private final long seed;
    private final Map<String, Random> sequences = new HashMap<>();

    public SeededBattleRandom(long seed) {
        this.seed = seed;
    }

    @Override
    public int nextInt(String sequence, int minimumInclusive, int maximumInclusive) {
        if (minimumInclusive > maximumInclusive) {
            throw new IllegalArgumentException("minimum must not exceed maximum");
        }
        if (minimumInclusive == maximumInclusive) {
            return minimumInclusive;
        }
        final long bound = (long) maximumInclusive - minimumInclusive + 1;
        if (bound > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("integer interval is too wide");
        }
        return minimumInclusive + sequence(sequence).nextInt((int) bound);
    }

    private Random sequence(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("sequence name must not be blank");
        }
        return sequences.computeIfAbsent(name, ignored -> new Random(mix(seed ^ stableHash(name))));
    }

    private static long stableHash(String value) {
        var hash = FNV_OFFSET_BASIS;
        for (int i = 0; i < value.length(); i++) {
            hash ^= value.charAt(i);
            hash *= FNV_PRIME;
        }
        return hash;
    }

    private static long mix(long value) {
        var mixed = value;
        mixed = (mixed ^ mixed >>> 30) * 0xbf58476d1ce4e5b9L;
        mixed = (mixed ^ mixed >>> 27) * 0x94d049bb133111ebL;
        return mixed ^ mixed >>> 31;
    }
}
