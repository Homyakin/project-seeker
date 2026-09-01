package ru.homyakin.seeker;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.utils.RandomUtils;

class RandomUtilsSeedTest {
    @Test
    void sameSeedReplaysEveryScopedSource() {
        final var first = sample(123_456_789L);
        final var second = sample(123_456_789L);

        Assertions.assertEquals(first, second);
        Assertions.assertEquals(4, first.uuid().version());
        Assertions.assertEquals(2, first.uuid().variant());
    }

    @Test
    void nestedScopeRestoresOuterSequenceAfterSuccessAndFailure() {
        final var expected = RandomUtils.withSeed(11L, () -> List.of(
            RandomUtils.getWithMax(10_000),
            RandomUtils.getWithMax(10_000),
            RandomUtils.getWithMax(10_000)
        ));

        final var actual = RandomUtils.withSeed(11L, () -> {
            final int first = RandomUtils.getWithMax(10_000);
            RandomUtils.withSeed(99L, () -> RandomUtils.getWithMax(10_000));
            final int second = RandomUtils.getWithMax(10_000);
            Assertions.assertThrows(IllegalStateException.class, () -> RandomUtils.withSeed(100L, () -> {
                throw new IllegalStateException("expected");
            }));
            final int third = RandomUtils.getWithMax(10_000);
            return List.of(first, second, third);
        });

        Assertions.assertEquals(expected, actual);
    }

    private static RandomSample sample(long seed) {
        return RandomUtils.withSeed(seed, () -> new RandomSample(
            RandomUtils.getInInterval(-10, 10),
            RandomUtils.getWithMax(1_000),
            RandomUtils.processChance(37),
            RandomUtils.bool(),
            RandomUtils.shuffle(List.of(1, 2, 3, 4, 5)),
            RandomUtils.randomUuid(),
            RandomUtils.getCharacteristicWithDeviation(100, 0.25)
        ));
    }

    private record RandomSample(
        int interval,
        int bounded,
        boolean chance,
        boolean bool,
        List<Integer> shuffled,
        UUID uuid,
        double characteristic
    ) {
    }
}
