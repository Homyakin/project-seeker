package ru.homyakin.seeker.game.battle.v3;

import ru.homyakin.seeker.game.personage.models.Personage;

public record PersonageBattleResult(
    Personage personage,
    PersonageBattleStats stats
) {
}
