package ru.homyakin.seeker.game.event.world_raid.entity;

import ru.homyakin.seeker.game.models.Money;

public record WorldRaidInfo(
    ActiveWorldRaid worldRaid,
    Money requiredForDonate
) {
}
