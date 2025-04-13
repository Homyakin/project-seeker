package ru.homyakin.seeker.game.event.world_raid.entity;

public record WorldRaidBattleInfo(
    int health,
    int attack,
    int defense,
    int strength,
    int agility,
    int wisdom
) {
}
