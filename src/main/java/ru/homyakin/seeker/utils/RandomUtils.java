package ru.homyakin.seeker.utils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.random.RandomGenerator;

public class RandomUtils {
    private static final RandomGenerator random = RandomGenerator.getDefault();

    public static Duration getRandomDuration(Duration minimum, Duration maximum) {
        return Duration.ofMillis(getInInterval(minimum.toMillis(), maximum.toMillis()));
    }

    public static long getInInterval(long start, long end) {
        if (start == end) {
            return start;
        }
        return random.nextLong(start, end + 1);
    }

    public static int getInInterval(int start, int end) {
        if (start == end) {
            return start;
        }
        return random.nextInt(start, end + 1);
    }

    public static <T> T getRandomElement(T[] array) {
        return array[random.nextInt(0, array.length)];
    }

    public static <T> List<T> shuffle(List<T> list) {
        final var modifiableList = new ArrayList<T>(list);
        Collections.shuffle(modifiableList);
        return modifiableList;
    }
}
