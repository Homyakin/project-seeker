package ru.homyakin.seeker.game.battle.skill;

public sealed interface ActiveSkill
    extends ItemSkill
    permits TurnSkill, DamageDealSkill {

}
