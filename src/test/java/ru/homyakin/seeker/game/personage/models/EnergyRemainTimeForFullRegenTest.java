package ru.homyakin.seeker.game.personage.models;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

public class EnergyRemainTimeForFullRegenTest {

    @Test
    public void Given_ZeroEnergy_When_NowEqualsLastChange_Then_RemainDurationEqualsFullRegenDuration() {
        final var lastChange = LocalDateTime.now();
        final var now = lastChange;
        final var fullRegenDuration = Duration.ofHours(1);

        final var energy = new Energy(0, lastChange, fullRegenDuration);

        final var result = energy.remainTimeForFullRegen(now);

        Assertions.assertEquals(fullRegenDuration, result);
    }

    @Test
    public void Given_ZeroEnergy_When_NowIsLastChangePlus1Minute_Then_RemainDurationEqualsFullRegenDurationMinus1Minute() {
        final var lastChange = LocalDateTime.now();
        final var now = lastChange.plusMinutes(1);
        final var fullRegenDuration = Duration.ofHours(1);

        final var energy = new Energy(0, lastChange, fullRegenDuration);

        final var result = energy.remainTimeForFullRegen(now);

        Assertions.assertEquals(fullRegenDuration.minusMinutes(1), result);
    }

    @Test
    public void Given_NotFullEnergy_When_NowEqualsLastChange_Then_RemainDurationEqualsFullRegenDurationMinusTimeToExistEnergy() {
        final var lastChange = LocalDateTime.now();
        final var now = lastChange;
        final var fullRegenDuration = Duration.ofMinutes(100);

        final var energy = new Energy(23, lastChange, fullRegenDuration);

        final var result = energy.remainTimeForFullRegen(now);

        Assertions.assertEquals(Duration.ofMinutes(77), result);
    }

    @Test
    public void Given_EnergyIsFull_When_NowEqualsLastChange_Then_RemainDurationEqualsZero() {
        final var lastChange = LocalDateTime.now();
        final var now = lastChange;
        final var fullRegenDuration = Duration.ofMinutes(100);

        final var energy = new Energy(100, lastChange, fullRegenDuration);

        final var result = energy.remainTimeForFullRegen(now);

        Assertions.assertEquals(Duration.ZERO, result);
    }

    @Test
    public void Given_EnergyIsFull_When_NowMoreThanLastChange_Then_RemainDurationEqualsZero() {
        final var lastChange = LocalDateTime.now();
        final var now = lastChange.plusMinutes(20);
        final var fullRegenDuration = Duration.ofMinutes(100);

        final var energy = new Energy(100, lastChange, fullRegenDuration);

        final var result = energy.remainTimeForFullRegen(now);

        Assertions.assertEquals(Duration.ZERO, result);
    }
}
