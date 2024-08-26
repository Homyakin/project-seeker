package ru.homyakin.seeker.game.personage.models;

import io.vavr.control.Either;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import ru.homyakin.seeker.game.personage.models.errors.NotEnoughEnergy;
import ru.homyakin.seeker.game.personage.models.errors.StillSame;
import ru.homyakin.seeker.utils.MathUtils;
import ru.homyakin.seeker.utils.TimeUtils;

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

    public Either<StillSame, Energy> regenIfNeeded(Duration timeForFullRegen, LocalDateTime now) {
        if (value >= MAX_ENERGY) {
            return Either.left(StillSame.INSTANCE);
        }
        final var millisPassed = Duration.between(lastChange, now).toMillis();
        final var millisToRegen1Energy = timeForFullRegen.toMillis() / MAX_ENERGY;
        final var increaseEnergy = MathUtils.doubleToIntWithMinMaxValues((double) millisPassed / millisToRegen1Energy);
        if (increaseEnergy > 0) {
            if (value + increaseEnergy >= MAX_ENERGY) {
                return Either.right(new Energy(MAX_ENERGY, now));
            } else {
                final int newHealth = value + increaseEnergy;
                final var millisToRegenIncreasedEnergy = increaseEnergy * millisToRegen1Energy;
                return Either.right(new Energy(newHealth, lastChange.plus(millisToRegenIncreasedEnergy, ChronoUnit.MILLIS)));
            }
        }
        return Either.left(StillSame.INSTANCE);
    }

    public boolean isGreaterOrEqual(int energyValue) {
        return value >= energyValue;
    }

    public Either<NotEnoughEnergy, Energy> reduce(int energyValue, LocalDateTime changeTime, Duration timeForFullRegen) {
        final var regenerated = regenIfNeeded(timeForFullRegen, changeTime)
            .fold(
                _ -> this,
                energy -> energy
            );
        /*
          Если энергия была максимальной, тогда дата изменения равна текущей дате, чтобы при следующей регенерации
          не восполнилось слишком много энергии
         */
        if (regenerated.value == MAX_ENERGY) {
            return Either.right(new Energy(regenerated.value - energyValue, changeTime));
        } else if (regenerated.value < energyValue) {
            return Either.left(NotEnoughEnergy.INSTANCE);
        } else {
            return Either.right(new Energy(regenerated.value - energyValue, regenerated.lastChange));
        }
    }

    private static final int MAX_ENERGY = 100;
}
