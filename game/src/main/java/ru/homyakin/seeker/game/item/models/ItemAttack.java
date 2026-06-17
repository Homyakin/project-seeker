package ru.homyakin.seeker.game.item.models;

public record ItemAttack(
    AttackType attackType,
    int range,
    int attack
) {
}
