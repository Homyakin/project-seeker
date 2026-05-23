package ru.homyakin.seeker.game.battle;

import java.util.UUID;

public record BattlePersonageInitSnapshot(
    UUID id,
    boolean firstTeam,
    int health,
    int lineIndex,
    BattleAdvanceDirection advanceDirection,
    int initiative,
    int initiativeGauge,
    int range,
    int totalThreat
) {
}
