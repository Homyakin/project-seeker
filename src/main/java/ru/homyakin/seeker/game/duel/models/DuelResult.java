package ru.homyakin.seeker.game.duel.models;

import ru.homyakin.seeker.game.battle.PersonageBattleResult;

public record DuelResult(
    PersonageBattleResult winner,
    PersonageBattleResult loser
) {
}
