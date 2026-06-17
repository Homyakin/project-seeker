package ru.homyakin.seeker.game.personage;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.personage.models.Energy;
import ru.homyakin.seeker.game.personage.models.errors.NotEnoughEnergy;
import ru.homyakin.seeker.utils.TimeUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class EnergyReduceTest {
    private final Duration regenDuration = Duration.of(200, ChronoUnit.MINUTES);

    @Test
    public void Given_FullEnergy_When_ReduceEnergy_Then_ReducedOnValueAndChangeTime() {
        // given
        final var energy = new Energy(
            100,
            LocalDateTime.of(2020, 12, 31, 0, 0),
            regenDuration
        );
        // when
        final var time = TimeUtils.moscowTime();
        final var result = energy.reduce(10, time);
        // then
        Assertions.assertTrue(result.isRight());
        Assertions.assertEquals(90, result.get().value());
        Assertions.assertEquals(time, result.get().lastChange());
    }

    @Test
    public void Given_EmptyEnergy_And_TimeLessThenRegen_When_ReduceEnergy_Then_NotEnoughEnergy() {
        // given
        final var time = LocalDateTime.of(2020, 12, 31, 0, 0);
        final var energy = new Energy(
            0,
            LocalDateTime.of(2020, 12, 31, 0, 0),
            regenDuration
        );
        // when
        final var reduceTime = time.plusMinutes(1);
        final var result = energy.reduce(10, reduceTime);
        // then
        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(NotEnoughEnergy.INSTANCE, result.getLeft());
    }

    @Test
    public void Given_NotEnough_And_TimeToRegen_When_ReduceEnergy_Then_EnergyReducedToValueMinusRegenerated() {
        // given
        final var time = TimeUtils.moscowTime();
        final var energy = new Energy(9, time, regenDuration);
        // when
        final var reduceTime = time.plusMinutes(4);
        final var result = energy.reduce(10, reduceTime);
        // then
        Assertions.assertTrue(result.isRight());
        Assertions.assertEquals(1, result.get().value());
        Assertions.assertEquals(reduceTime, result.get().lastChange());
    }
}
