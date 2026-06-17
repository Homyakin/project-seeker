package ru.homyakin.seeker.test_utils;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;

public class TestRandom {
    private static final UniformRandomProvider random = RandomSource.JDK.create();
    private static final RandomStringUtils randomStringUtils = RandomStringUtils.insecure();

    public static long nextLong() {
        return random.nextLong();
    }

    public static int nextInt() {
        return random.nextInt();
    }

    public static String randomNumeric(int length) {
        return randomStringUtils.nextNumeric(length);
    }

    public static String randomAlphanumeric(int length) {
        return randomStringUtils.nextAlphanumeric(length);
    }

    public static String random(int length) {
        return randomStringUtils.next(length);
    }
}
