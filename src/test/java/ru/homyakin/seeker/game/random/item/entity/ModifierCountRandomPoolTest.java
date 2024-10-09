package ru.homyakin.seeker.game.random.item.entity;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ModifierCountRandomPoolTest {
    @Test
    public void When_GeneratePoolWithParams_Then_PoolContainsRequiredCount() {
        final var expectedZero = 7;
        final var expectedOne = 6;
        final var expectedTwo = 5;

        final var pool = ModifierCountRandomPool.generate(expectedZero, expectedOne, expectedTwo);

        final var resultZero = pool.pool().stream().filter(it -> it == 0).count();
        final var resultOne = pool.pool().stream().filter(it -> it == 1).count();
        final var resultTwo = pool.pool().stream().filter(it -> it == 2).count();

        Assertions.assertEquals(expectedZero, resultZero);
        Assertions.assertEquals(expectedOne, resultOne);
        Assertions.assertEquals(expectedTwo, resultTwo);
    }
}
