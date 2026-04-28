package ru.homyakin.seeker.game.battle.v3.two_team;

import ru.homyakin.seeker.game.personage.models.Personage;

public record PersonageBattleResult(
    Personage personage,
    PersonageBattleStats stats
) {
}
