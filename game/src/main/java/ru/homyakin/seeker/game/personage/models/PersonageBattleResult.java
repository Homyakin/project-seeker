package ru.homyakin.seeker.game.personage.models;

import ru.homyakin.seeker.game.battle.BattlePersonageStats;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.models.StormShards;

import java.util.Optional;

public record PersonageBattleResult(
    PersonageId personageId,
    long launchedEventId,
    BattlePersonageStats stats,
    Money reward,
    StormShards stormShards,
    Optional<Long> generatedItemId,
    Optional<Long> generatedContrabandId
) {
}
