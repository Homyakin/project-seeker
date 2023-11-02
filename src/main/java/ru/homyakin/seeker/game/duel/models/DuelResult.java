package ru.homyakin.seeker.game.duel.models;

import ru.homyakin.seeker.game.battle.PersonageResult;

public record DuelResult(
    PersonageResult winner,
    PersonageResult loser
) {
}
