package ru.homyakin.seeker.game.personage.models.errors;

import ru.homyakin.seeker.game.models.StormShards;

public record NotEnoughStormShards(
    StormShards needed
) {
}
