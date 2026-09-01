package ru.homyakin.seeker.game.battle.simulation;

import java.util.Objects;
import java.util.function.Supplier;

public record CombatSimulationRequest(
    String raidType,
    String loadout,
    String composition,
    int difficulty,
    int partySize,
    int iterations,
    int maxRounds,
    long seed,
    Supplier<CombatSimulationTeams> teamsFactory
) {
    public CombatSimulationRequest {
        raidType = requireText(raidType, "raidType");
        loadout = requireText(loadout, "loadout");
        composition = requireText(composition, "composition");
        if (difficulty <= 0) {
            throw new IllegalArgumentException("difficulty must be positive");
        }
        if (partySize <= 0) {
            throw new IllegalArgumentException("partySize must be positive");
        }
        if (iterations <= 0) {
            throw new IllegalArgumentException("iterations must be positive");
        }
        if (maxRounds <= 0) {
            throw new IllegalArgumentException("maxRounds must be positive");
        }
        Objects.requireNonNull(teamsFactory, "teamsFactory");
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
