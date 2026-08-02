package ru.homyakin.seeker.game.shop.models;

import ru.homyakin.seeker.game.item.storm.StormEnhanceProbabilities;
import ru.homyakin.seeker.game.models.StormShards;

public record StormEnhanceAction(
    StormShards cost,
    StormEnhanceProbabilities probabilities,
    int currentLevel,
    int nextLevel
) {
}
