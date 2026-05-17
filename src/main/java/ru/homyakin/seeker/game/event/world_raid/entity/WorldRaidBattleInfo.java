package ru.homyakin.seeker.game.event.world_raid.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WorldRaidBattleInfo(
    int health,
    int attack,
    int defense
) {
}
