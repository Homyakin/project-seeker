package ru.homyakin.seeker.game.battle;

import java.util.Map;
import java.util.UUID;

public record BattleResult(
    BattleInitState initState,
    BattleActionLog actionLog,
    int rounds,
    boolean firstWin,
    Map<UUID, BattlePersonageStats> personageStats
) {
}
