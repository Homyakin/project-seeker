package ru.homyakin.seeker.game.personage.models;

import ru.homyakin.seeker.game.battle.PersonageBattleStats;
import ru.homyakin.seeker.game.models.Money;

import java.util.Optional;

public record PersonageRaidSavedResult(
    PersonageId personageId,
    long launchedEventId,
    PersonageBattleStats stats,
    Money reward,
    Optional<Long> generatedItemId
) {
}
