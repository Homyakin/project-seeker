package ru.homyakin.seeker.game.duel.models;

import ru.homyakin.seeker.game.battle.v4.BattlePersonageStats;
import ru.homyakin.seeker.game.personage.models.Personage;

public record DuelPersonageResult(
    Personage personage,
    BattlePersonageStats stats
) {
}
