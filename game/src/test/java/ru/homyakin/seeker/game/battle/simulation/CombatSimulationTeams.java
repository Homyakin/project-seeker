package ru.homyakin.seeker.game.battle.simulation;

import java.util.List;
import java.util.Objects;
import ru.homyakin.seeker.game.battle.BattlePersonage;

public record CombatSimulationTeams(
    List<BattlePersonage> evaluatedTeam,
    List<BattlePersonage> opponents
) {
    public CombatSimulationTeams {
        Objects.requireNonNull(evaluatedTeam, "evaluatedTeam");
        Objects.requireNonNull(opponents, "opponents");
        evaluatedTeam = List.copyOf(evaluatedTeam);
        opponents = List.copyOf(opponents);
        if (evaluatedTeam.isEmpty() || opponents.isEmpty()) {
            throw new IllegalArgumentException("Both simulation teams must be non-empty");
        }
    }
}
