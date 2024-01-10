package ru.homyakin.seeker.game.personage.models;

import ru.homyakin.seeker.game.battle.PersonageBattleStats;
import ru.homyakin.seeker.game.models.Money;

public record PersonageRaidSavedResult(
    PersonageId personageId,
    long launchedEventId,
    PersonageBattleStats stats,
    Money reward
) {
}
