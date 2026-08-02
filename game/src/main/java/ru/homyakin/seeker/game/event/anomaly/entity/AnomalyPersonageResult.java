package ru.homyakin.seeker.game.event.anomaly.entity;

import ru.homyakin.seeker.game.battle.BattlePersonageStats;
import ru.homyakin.seeker.game.personage.models.Personage;

public record AnomalyPersonageResult(
    Personage personage,
    BattlePersonageStats stats,
    AnomalyReward reward
) {
}
