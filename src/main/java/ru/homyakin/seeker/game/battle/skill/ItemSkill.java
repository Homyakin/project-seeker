package ru.homyakin.seeker.game.battle.skill;

import ru.homyakin.seeker.game.battle.BattlePersonage;

public sealed interface ItemSkill permits ActiveSkill {

    /**
     * Expected combat value in abstract attack-equivalent units for
     * {@link BattlePersonage#power()}.
     * Must use the same numeric parameters as actual combat logic (damage, heal, chances, cooldowns, etc.).
     */
    double skillPowerRating(SkillPowerInputs inputs);
}
