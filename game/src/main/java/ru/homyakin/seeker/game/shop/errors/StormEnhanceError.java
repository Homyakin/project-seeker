package ru.homyakin.seeker.game.shop.errors;

import ru.homyakin.seeker.game.models.StormShards;

public sealed interface StormEnhanceError {
    record NotEnoughStormShards(StormShards required) implements StormEnhanceError {
    }

    enum NoSuchItem implements StormEnhanceError {
        INSTANCE
    }

    enum MaxLevel implements StormEnhanceError {
        INSTANCE
    }
}
