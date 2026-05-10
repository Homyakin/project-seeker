package ru.homyakin.seeker.game.battle.v4.skill;

/**
 * Snapshot of personage stats used to rate skills for {@link ru.homyakin.seeker.game.battle.v4.BattlePersonage#power()}.
 */
public record SkillPowerInputs(
    int dodgeChancePercent,
    int critChancePercent,
    int maxHealth
) {
}
