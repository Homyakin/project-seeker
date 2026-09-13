package ru.homyakin.seeker.game.shop.errors;

import ru.homyakin.seeker.game.models.StormShards;

public sealed interface StormEnhanceError {
    record NotEnoughStormShards(StormShards required) implements StormEnhanceError {
    }

    record StaleItemState(int actualLevel, long actualRevision) implements StormEnhanceError {
    }

    record TechnicalLimitReached(int maxLevel) implements StormEnhanceError {
    }

    enum NoSuchItem implements StormEnhanceError {
        INSTANCE
    }
}
