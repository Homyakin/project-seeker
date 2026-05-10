package ru.homyakin.seeker.game.battle.v4.skill;

public sealed interface ItemSkill permits ActiveSkill {

    /**
     * Expected combat value in abstract attack-equivalent units for
     * {@link ru.homyakin.seeker.game.battle.v4.BattlePersonage#power()}.
     * Must use the same numeric parameters as actual combat logic (damage, heal, chances, cooldowns, etc.).
     */
    double skillPowerRating(SkillPowerInputs inputs);
}
