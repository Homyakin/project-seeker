package ru.homyakin.seeker.game.personage;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.personage.models.Energy;
import ru.homyakin.seeker.utils.TimeUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class EnergyReduceTest {
    private final Duration regenDuration = Duration.of(200, ChronoUnit.MINUTES);

    @Test
    public void Given_FullEnergy_When_ReduceEnergy_Then_ReducedOnValue() {
        // given
        final var energy = new Energy(100, LocalDateTime.of(2020, 12, 31, 0, 0));
        // when
        final var time = TimeUtils.moscowTime();
        final var result = energy.reduce(10, time, regenDuration);
        // then
        Assertions.assertEquals(90, result.value());
        Assertions.assertEquals(time, result.lastChange());
    }

    @Test
    public void Given_EmptyEnergy_And_TimeLessThenRegen_When_ReduceEnergy_Then_EnergyStillEmpty() {
        // given
        final var time = LocalDateTime.of(2020, 12, 31, 0, 0);
        final var energy = new Energy(0, LocalDateTime.of(2020, 12, 31, 0, 0));
        // when
        final var reduceTime = time.plusMinutes(1);
        final var result = energy.reduce(10, reduceTime, regenDuration);
        // then
        Assertions.assertEquals(0, result.value());
        Assertions.assertEquals(reduceTime, result.lastChange());
    }

    @Test
    public void Given_NotFullEnergy_And_TimeToRegen_When_ReduceEnergy_Then_EnergyReducedToValueMinusRegenerated() {
        // given
        final var time = TimeUtils.moscowTime();
        final var energy = new Energy(50, time);
        // when
        final var reduceTime = time.plusMinutes(2);
        final var result = energy.reduce(10, reduceTime, regenDuration);
        // then
        Assertions.assertEquals(41, result.value());
        Assertions.assertEquals(reduceTime, result.lastChange());
    }
}
