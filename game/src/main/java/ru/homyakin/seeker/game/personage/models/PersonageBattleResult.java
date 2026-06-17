package ru.homyakin.seeker.game.personage.models;

import ru.homyakin.seeker.game.battle.BattlePersonageStats;
import ru.homyakin.seeker.game.models.Money;

import java.util.Optional;

public record PersonageBattleResult(
    PersonageId personageId,
    long launchedEventId,
    BattlePersonageStats stats,
    Money reward,
    Optional<Long> generatedItemId,
    Optional<Long> generatedContrabandId
) {
}
