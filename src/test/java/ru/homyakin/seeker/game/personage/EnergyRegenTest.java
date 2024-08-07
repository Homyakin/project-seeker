package ru.homyakin.seeker.game.personage;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.personage.models.Energy;
import ru.homyakin.seeker.game.personage.models.errors.EnergyStillSame;
import ru.homyakin.seeker.utils.RandomUtils;
import ru.homyakin.seeker.utils.TimeUtils;

public class EnergyRegenTest {
    private final Duration regenDuration = Duration.of(200, ChronoUnit.MINUTES);

    @Test
    public void Given_FullEnergy_When_RegenEnergy_Then_EnergyStillSame() {
        // given
        final var energy = new Energy(100, LocalDateTime.of(2020, 12, 31, 0, 0));
        // when
        final var result = energy.regenIfNeeded(regenDuration, TimeUtils.moscowTime());
        // then
        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(EnergyStillSame.INSTANCE, result.getLeft());
    }

    @Test
    public void Given_EmptyEnergy_When_RegenAtSameTime_Then_EnergyStillSame() {
        // given
        final var time = TimeUtils.moscowTime();
        final var energy = new Energy(0, time);
        // when
        final var result = energy.regenIfNeeded(regenDuration, time);
        // then
        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(EnergyStillSame.INSTANCE, result.getLeft());
    }

    @Test
    public void Given_EmptyEnergy_When_RegenAfter1Minute_Then_EnergyStillSame() {
        // given
        final var time = TimeUtils.moscowTime();
        final var energy = new Energy(0, time);
        // when
        final var regenTime = time.plusMinutes(1);
        final var result = energy.regenIfNeeded(regenDuration, regenTime);
        // then
        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(EnergyStillSame.INSTANCE, result.getLeft());
    }

    @Test
    public void Given_EmptyEnergy_When_RegenAfter2Minutes_Then_EnergyValueIs1AndLastChangeIsAtRegenTime() {
        // given
        final var time = TimeUtils.moscowTime();
        final var energy = new Energy(0, time);
        // when
        final var regenTime = time.plusMinutes(2);
        final var result = energy.regenIfNeeded(regenDuration, regenTime);
        // then
        Assertions.assertTrue(result.isRight());
        Assertions.assertEquals(1, result.get().value());
        Assertions.assertEquals(regenTime, result.get().lastChange());
    }

    @Test
    public void Given_EmptyEnergy_When_RegenAfter200Minutes_Then_EnergyValueIsMaxAndLastChangeIsAtRegenTime() {
        // given
        final var time = TimeUtils.moscowTime();
        final var energy = new Energy(0, time);
        // when
        final var regenTime = time.plusMinutes(200);
        final var result = energy.regenIfNeeded(regenDuration, regenTime);
        // then
        Assertions.assertTrue(result.isRight());
        Assertions.assertEquals(MAX_ENERGY, result.get().value());
        Assertions.assertEquals(regenTime, result.get().lastChange());
    }

    @Test
    public void Given_EmptyEnergy_When_RegenAfterMoreThan200Minutes_Then_EnergyValueIsMaxAndLastChangeIsAtRegenTime() {
        // given
        final var time = TimeUtils.moscowTime();
        final var energy = new Energy(0, time);
        // when
        final var regenTime = time.plusMinutes(RandomUtils.getInInterval(201, Integer.MAX_VALUE - 1));
        final var result = energy.regenIfNeeded(regenDuration, regenTime);
        // then
        Assertions.assertTrue(result.isRight());
        Assertions.assertEquals(MAX_ENERGY, result.get().value());
        Assertions.assertEquals(regenTime, result.get().lastChange());
    }

    @Test
    public void Given_NotFullEnergy_When_RegenAfterMoreThan2Minutes_Then_EnergyValueWasIncreasedBy1AndLastChangeIsAtRegenTime() {
        // given
        final var time = TimeUtils.moscowTime();
        final var energy = new Energy(RandomUtils.getInInterval(10, 90), time);
        // when
        final var regenTime = time.plusMinutes(2);
        final var result = energy.regenIfNeeded(regenDuration, regenTime);
        // then
        Assertions.assertTrue(result.isRight());
        Assertions.assertEquals(energy.value() + 1, result.get().value());
        Assertions.assertEquals(regenTime, result.get().lastChange());
    }

    @Test
    public void Given_NotFullEnergy_When_RegenAfter2MinutesAnd30Seconds_Then_EnergyValueWasIncreasedBy1AndLastChangeIncreasedBy2Minutes() {
        // given
        final var time = TimeUtils.moscowTime();
        final var energy = new Energy(RandomUtils.getInInterval(10, 90), time);
        // when
        final var regenTime = time.plusMinutes(2).plusSeconds(30);
        final var result = energy.regenIfNeeded(regenDuration, regenTime);
        // then
        Assertions.assertTrue(result.isRight());
        Assertions.assertEquals(energy.value() + 1, result.get().value());
        Assertions.assertEquals(regenTime.minusSeconds(30), result.get().lastChange());
    }

    private static final int MAX_ENERGY = 100;
}
