package ru.homyakin.seeker.game.personage.models;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EnergyRecoveryTimeTest {

    @Test
    public void When_EnergyIsFull_Then_EnergyRecoveryTimeIsEmpty() {
        final var value = 100;
        final var lastChange = LocalDateTime.now();
        final var totalFullRegenDuration = Duration.ofHours(10);

        final var energy = new Energy(value, lastChange, totalFullRegenDuration);

        final var recoveryTime = energy.energyRecoveryTime();

        assertTrue(recoveryTime.isEmpty());
    }

    @Test
    public void When_EnergyIsHalfOfMax_Then_EnergyRecoveryTimeLastChangePlusHalfOfTotalFullRegenDuration() {
        int value = 50;
        final var lastChange = LocalDateTime.now();
        final var totalFullRegenDuration = Duration.ofHours(10);

        final var energy = new Energy(value, lastChange, totalFullRegenDuration);

        final var recoveryTime = energy.energyRecoveryTime();

        assertTrue(recoveryTime.isPresent());
        assertEquals(lastChange.plusHours(5), recoveryTime.get());
    }

    @Test
    public void When_EnergyIsZero_Then_EnergyRecoveryTimeLastChangePlusTotalFullRegenDuration() {
        int value = 0;
        final var lastChange = LocalDateTime.now();
        final var totalFullRegenDuration = Duration.ofHours(10);

        final var energy = new Energy(value, lastChange, totalFullRegenDuration);

        final var recoveryTime = energy.energyRecoveryTime();

        assertTrue(recoveryTime.isPresent());
        assertEquals(lastChange.plusHours(10), recoveryTime.get());
    }
}
