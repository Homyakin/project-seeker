package ru.homyakin.seeker.utils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.random.RandomGenerator;
import org.apache.commons.math3.distribution.AbstractRealDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.homyakin.seeker.utils.models.IntRange;

public class RandomUtils {
    private static final Logger logger = LoggerFactory.getLogger(RandomUtils.class);
    private static final RandomGenerator random = RandomGenerator.getDefault();
    private static final AbstractRealDistribution characteristicsRandom = new NormalDistribution(0.5, 0.2);

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
        return random.nextLong(start, end + 1);
    }

    public static int getInInterval(int start, int end) {
        if (start >= end) {
            return start;
        }
        return random.nextInt(start, end + 1);
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
            LocalDateTime.ofEpochSecond(random.nextLong(startSeconds, endSeconds), 0, start.getOffset()),
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
        return array[random.nextInt(0, array.length)];
    }

    public static <T> T getRandomElement(List<T> list) {
        return list.get(random.nextInt(0, list.size()));
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
        Collections.shuffle(modifiableList);
        return modifiableList;
    }

    public static boolean bool() {
        return random.nextBoolean();
    }

    private static double characteristicSampleFrom0To1() {
        final var result = characteristicsRandom.sample();
        if (result < 0) {
            return 0;
        } else if (result > 1) {
            return 1;
        }
        return result;
    }
}
