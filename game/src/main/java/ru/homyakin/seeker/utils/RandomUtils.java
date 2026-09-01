package ru.homyakin.seeker.utils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.ToIntFunction;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

import org.apache.commons.math3.distribution.AbstractRealDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.homyakin.seeker.utils.models.IntRange;

public class RandomUtils {
    private static final Logger logger = LoggerFactory.getLogger(RandomUtils.class);
    private static final RandomGenerator defaultRandom = RandomGenerator.getDefault();
    private static final AbstractRealDistribution characteristicsRandom = new NormalDistribution(0.5, 0.2);
    private static final ThreadLocal<Random> scopedRandom = new ThreadLocal<>();

    /**
     * Runs the complete action with a deterministic, thread-confined random stream.
     * Nested scopes restore the outer stream and exceptions never leak the seeded state.
     */
    public static <T> T withSeed(long seed, Supplier<T> action) {
        final var previous = scopedRandom.get();
        scopedRandom.set(new Random(seed));
        try {
            return action.get();
        } finally {
            if (previous == null) {
                scopedRandom.remove();
            } else {
                scopedRandom.set(previous);
            }
        }
    }

    public static Duration getRandomDuration(Duration minimum, Duration maximum) {
        return Duration.ofMillis(getInInterval(minimum.toMillis(), maximum.toMillis()));
    }

    public static int getInInterval(IntRange range) {
        return getInInterval(range.min(), range.max());
    }

    public static long getInInterval(long start, long end) {
        if (start >= end) {
            return start;
        }
        return currentRandom().nextLong(start, end + 1);
    }

    public static int getInInterval(int start, int end) {
        if (start >= end) {
            return start;
        }
        return currentRandom().nextInt(start, end + 1);
    }

    public static int getWithMax(int max) {
        return currentRandom().nextInt(max);
    }

    public static int getInPercentRange(int start, double percent) {
        final var fraction = percent / 100.0;
        return getInInterval(
            (int) Math.round(start * (1 - fraction)),
            (int) Math.round(start * (1 + fraction))
        );
    }

    public static OffsetDateTime getInInterval(OffsetDateTime start, OffsetDateTime end) {
        final var startSeconds = start.toEpochSecond();
        final var endSeconds = end.toEpochSecond();
        if (startSeconds >= endSeconds) {
            return start;
        }
        return OffsetDateTime.of(
            LocalDateTime.ofEpochSecond(currentRandom().nextLong(startSeconds, endSeconds), 0, start.getOffset()),
            start.getOffset()
        );
    }

    public static boolean processChance(int percent) {
        if (percent >= 100) {
            logger.debug("Requested percent {} >= 100", percent);
            return true;
        }
        final var result = getInInterval(1, 100);
        logger.debug("Requested percent {}; result {}", percent, result);
        return result <= percent;
    }

    public static <T> T getRandomElement(T[] array) {
        return array[currentRandom().nextInt(0, array.length)];
    }

    public static <T> T getRandomElement(List<T> list) {
        return list.get(currentRandom().nextInt(0, list.size()));
    }

    public static <T> T getRandomElement(Set<T> set) {
        var value = currentRandom().nextInt(0, set.size());
        for (final var element: set) {
            if (value == 0) {
                return element;
            }
            --value;
        }
        throw new IllegalStateException("can't get random element for set");
    }

    public static <T> T getRandomByWeights(List<T> list, ToIntFunction<T> weightExtractor) {
        final var totalWeight = list.stream().mapToInt(weightExtractor).sum();
        var roll = getInInterval(1, totalWeight);
        for (final var item : list) {
            roll -= weightExtractor.applyAsInt(item);
            if (roll <= 0) {
                return item;
            }
        }
        return list.getLast();
    }

    /**
     * @param value     базовое значение характеристики
     * @param deviation отклонение. Возможные значения 0..1
     * @return характеристика с отклонением
     */
    public static double getCharacteristicWithDeviation(double value, double deviation) {
        return getCharacteristic(value * (1 - deviation), value * (1 + deviation));
    }

    private static double getCharacteristic(double min, double max) {
        if (max < min) {
            throw new IllegalArgumentException("Max %f is less than min %f".formatted(max, min));
        }
        final var percentValue = characteristicSampleFrom0To1();
        /*
        * Получаем значение от 0 до 1.
        * Считаем разницу между min и max.
        * Считаем, какой процент от разницы нужно прибавить к min и округляем
         */
        final var diff = max - min;
        return min + diff * percentValue;
    }

    public static <T> List<T> shuffle(List<T> list) {
        final var modifiableList = new ArrayList<T>(list);
        final var seeded = scopedRandom.get();
        if (seeded == null) {
            Collections.shuffle(modifiableList);
        } else {
            Collections.shuffle(modifiableList, seeded);
        }
        return modifiableList;
    }

    public static boolean bool() {
        return currentRandom().nextBoolean();
    }

    /**
     * Production keeps the JDK UUID source; a seeded scope produces deterministic RFC 4122 version-4 UUIDs.
     */
    public static UUID randomUuid() {
        final var seeded = scopedRandom.get();
        if (seeded == null) {
            return UUID.randomUUID();
        }
        var mostSignificantBits = seeded.nextLong();
        var leastSignificantBits = seeded.nextLong();
        mostSignificantBits = mostSignificantBits & 0xffffffffffff0fffL | 0x0000000000004000L;
        leastSignificantBits = leastSignificantBits & 0x3fffffffffffffffL | 0x8000000000000000L;
        return new UUID(mostSignificantBits, leastSignificantBits);
    }

    private static double characteristicSampleFrom0To1() {
        final var seeded = scopedRandom.get();
        final var result = seeded == null
            ? characteristicsRandom.sample()
            : seeded.nextGaussian(0.5, 0.2);
        if (result < 0) {
            return 0;
        } else if (result > 1) {
            return 1;
        }
        return result;
    }

    private static RandomGenerator currentRandom() {
        final var seeded = scopedRandom.get();
        return seeded == null ? defaultRandom : seeded;
    }
}
