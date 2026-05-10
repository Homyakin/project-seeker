package ru.homyakin.seeker.game.battle.v4.skill;

public sealed interface ActiveSkill
    extends ItemSkill
    permits TurnSkill, DamageDealSkill {

}
