package ru.homyakin.seeker.game.battle.skill;

import ru.homyakin.seeker.game.battle.BattlePersonage;

/**
 * Snapshot of personage stats used to rate skills for {@link BattlePersonage#power()}.
 */
public record SkillPowerInputs(
    int dodgeChancePercent,
    int critChancePercent,
    int baseAttack,
    int maxHealth,
    double expectedDamagePerTurn
) {
}
