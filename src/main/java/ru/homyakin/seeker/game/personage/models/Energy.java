package ru.homyakin.seeker.game.personage.models;

import io.vavr.control.Either;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import ru.homyakin.seeker.game.personage.models.errors.NotEnoughEnergy;
import ru.homyakin.seeker.game.personage.models.errors.StillSame;
import ru.homyakin.seeker.utils.MathUtils;

public record Energy(
    int value,
    LocalDateTime lastChange,
    Duration totalFullRegenDuration
) {
    public Energy {
        if (value < 0) {
            throw new IllegalArgumentException("energy must be more than zero");
        }
    }

    public Either<StillSame, Energy> regenIfNeeded(LocalDateTime now) {
        if (value >= MAX_ENERGY) {
            return Either.left(StillSame.INSTANCE);
        }
        final var millisPassed = Duration.between(lastChange, now).toMillis();
        final var millisToRegen1Energy = millisToRegen1Energy();
        final var increaseEnergy = MathUtils.doubleToIntWithMinMaxValues((double) millisPassed / millisToRegen1Energy);
        if (increaseEnergy > 0) {
            if (value + increaseEnergy >= MAX_ENERGY) {
                return Either.right(new Energy(MAX_ENERGY, now, totalFullRegenDuration));
            } else {
                final int newHealth = value + increaseEnergy;
                final var millisToRegenIncreasedEnergy = increaseEnergy * millisToRegen1Energy;
                return Either.right(
                    new Energy(newHealth, lastChange.plus(millisToRegenIncreasedEnergy, ChronoUnit.MILLIS), totalFullRegenDuration)
                );
            }
        }
        return Either.left(StillSame.INSTANCE);
    }

    public Duration remainTimeForFullRegen(LocalDateTime now) {
        final var passedMillis = Duration.between(lastChange, now).toMillis();
        final var requiredMillis = (MAX_ENERGY - value) * millisToRegen1Energy() - passedMillis;
        return Duration.ofMillis(Math.max(requiredMillis, 0));
    }

    public Optional<LocalDateTime> energyRecoveryTime() {
        if (isFull()) {
            return Optional.empty();
        }
        final var requiredDuration = Duration.ofMillis((MAX_ENERGY - value) * millisToRegen1Energy());
        return Optional.of(lastChange.plus(requiredDuration));
    }

    public boolean isFull() {
        return value == MAX_ENERGY;
    }

    private long millisToRegen1Energy() {
        return totalFullRegenDuration.toMillis() / MAX_ENERGY;
    }

    public boolean isGreaterOrEqual(int energyValue) {
        return value >= energyValue;
    }

    public Either<NotEnoughEnergy, Energy> reduce(int energyValue, LocalDateTime changeTime) {
        final var regenerated = regenIfNeeded(changeTime)
            .fold(
                _ -> this,
                energy -> energy
            );
        /*
          Если энергия была максимальной, тогда дата изменения равна текущей дате, чтобы при следующей регенерации
          не восполнилось слишком много энергии
         */
        if (regenerated.value == MAX_ENERGY) {
            return Either.right(new Energy(regenerated.value - energyValue, changeTime, totalFullRegenDuration));
        } else if (regenerated.value < energyValue) {
            return Either.left(NotEnoughEnergy.INSTANCE);
        } else {
            return Either.right(new Energy(regenerated.value - energyValue, regenerated.lastChange, totalFullRegenDuration));
        }
    }

    private static final int MAX_ENERGY = 100;
}
