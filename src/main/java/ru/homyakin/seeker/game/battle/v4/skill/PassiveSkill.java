package ru.homyakin.seeker.game.battle.v4.skill;

public record PassiveSkill(
    int percentAttack,
    int percentDefence,
    int percentCrit,
    int percentDodge,
    int percentSpeed
) implements ItemSkill {
}
