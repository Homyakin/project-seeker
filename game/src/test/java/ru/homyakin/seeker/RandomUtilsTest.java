package ru.homyakin.seeker;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.utils.RandomUtils;

public class RandomUtilsTest {
    @Test
    public void When_GetCharacteristicWithDeviation_Then_ResultBetweenMinAndMax() {
        final var value = 10;
        final var deviation = 0.5;
        final var result = RandomUtils.getCharacteristicWithDeviation(value, deviation);
        final var min = value * (1 - deviation);
        final var max = value * (1 + deviation);
        Assertions.assertTrue(result >= min && result <= max);
    }
}
