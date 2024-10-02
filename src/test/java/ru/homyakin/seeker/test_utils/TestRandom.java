package ru.homyakin.seeker.test_utils;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;

public class TestRandom {
    private static final UniformRandomProvider random = RandomSource.JDK.create();

    public static long nextLong() {
        return random.nextLong();
    }

    public static int nextInt() {
        return random.nextInt();
    }
}
