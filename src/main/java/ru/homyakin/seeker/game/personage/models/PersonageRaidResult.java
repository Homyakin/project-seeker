package ru.homyakin.seeker.game.personage.models;

import ru.homyakin.seeker.game.battle.PersonageBattleStats;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.models.Personage;

public record PersonageRaidResult(
    Personage personage,
    PersonageBattleStats stats,
    Money reward
) {
}
