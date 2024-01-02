package ru.homyakin.seeker.game.personage.models;

import io.vavr.control.Either;
import java.time.Duration;
import java.time.LocalDateTime;
import ru.homyakin.seeker.game.personage.models.errors.EnergyStillSame;
import ru.homyakin.seeker.utils.MathUtils;
import ru.homyakin.seeker.utils.TimeUtils;
import ru.homyakin.seeker.utils.models.Success;

public record Energy(
    int value,
    LocalDateTime lastChange
) {
    public Energy {
        if (value < 0) {
            throw new IllegalArgumentException("energy must be more than zero");
        }
    }

    public static Energy createDefault() {
        return new Energy(100, TimeUtils.moscowTime());
    }

    public static Energy createZero(LocalDateTime changeTime) {
        return new Energy(0, changeTime);
    }

    public float percent() {
        return (float) value / MAX_ENERGY;
    }

    public Either<Integer, Success> isEnoughForEvent() {
        if (value >= MIN_ENERGY_FOR_EVENT) {
            return Either.right(Success.INSTANCE);
        } else {
            return Either.left(MIN_ENERGY_FOR_EVENT);
        }
    }

    public Either<EnergyStillSame, Energy> regenIfNeeded() {
        if (value >= MAX_ENERGY) {
            return Either.left(EnergyStillSame.INSTANCE);
        }
        final var time = TimeUtils.moscowTime();
        final var minutesPass = Duration.between(lastChange, time).toMinutes();
        final var increaseEnergy = MathUtils.doubleToIntWithMinMaxValues(
            ((double) MAX_ENERGY) / 200 * minutesPass
        );
        if (increaseEnergy > 0) {
            final int newHealth = Math.min(value + increaseEnergy, MAX_ENERGY);
            return Either.right(new Energy(newHealth, lastChange.plusMinutes(minutesPass)));
        }
        return Either.left(EnergyStillSame.INSTANCE);
    }

    private static final int MAX_ENERGY = 100;
    private static final int MIN_ENERGY_FOR_EVENT = 50;
}
