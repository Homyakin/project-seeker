package ru.homyakin.seeker;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.utils.RandomUtils;

public class RandomUtilsTest {
    @Test
    public void When_GetCharacteristic_Then_ResultBetweenMinAndMax() {
        final var min = 5;
        final var max = 15;
        final var result = RandomUtils.getCharacteristic(min, max);
        Assertions.assertTrue(result >= min && result <= max);
    }
}
