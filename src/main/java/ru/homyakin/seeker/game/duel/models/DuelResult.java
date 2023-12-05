package ru.homyakin.seeker.game.duel.models;

import ru.homyakin.seeker.game.battle.BattlePersonage;

public record DuelResult(
    BattlePersonage winner,
    BattlePersonage loser
) {
}
