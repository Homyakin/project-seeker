package ru.homyakin.seeker.game.shop.models;

import ru.homyakin.seeker.game.item.storm.ItemEnhancementDelta;
import ru.homyakin.seeker.game.item.storm.StormEnhanceProbabilities;
import ru.homyakin.seeker.game.models.StormShards;

public record StormEnhanceAction(
    StormShards cost,
    StormEnhanceProbabilities probabilities,
    ItemEnhancementDelta delta,
    int currentLevel,
    int nextLevel,
    long currentRevision
) {
}
