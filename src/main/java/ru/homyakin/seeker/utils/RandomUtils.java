package ru.homyakin.seeker.utils;

import java.time.Duration;
import java.util.random.RandomGenerator;

public class RandomUtils {
    private static final RandomGenerator random = RandomGenerator.getDefault();

    public static Duration getRandomDuration(Duration minimum, Duration maximum) {
        return Duration.ofMillis(getInInterval(minimum.toMillis(), maximum.toMillis()));
    }

    public static long getInInterval(long start, long end) {
        return random.nextLong(start, end + 1);
    }

    public static int getInInterval(int start, int end) {
        return random.nextInt(start, end + 1);
    }
}
