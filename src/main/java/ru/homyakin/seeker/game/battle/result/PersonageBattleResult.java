package ru.homyakin.seeker.game.battle.result;

import ru.homyakin.seeker.game.battle.BattlePersonageStats;
import ru.homyakin.seeker.game.personage.models.Personage;

public record PersonageBattleResult(
    Personage personage,
    BattlePersonageStats stats
) {
}
