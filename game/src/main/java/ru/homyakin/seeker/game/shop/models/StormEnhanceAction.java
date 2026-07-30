package ru.homyakin.seeker.game.shop.models;

import ru.homyakin.seeker.game.models.StormShards;

public record StormEnhanceAction(
    StormShards cost,
    int successPercent,
    int currentLevel,
    int nextLevel
) {
}
