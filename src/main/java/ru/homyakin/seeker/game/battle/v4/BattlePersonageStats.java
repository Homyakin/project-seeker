package ru.homyakin.seeker.game.battle.v4;

public record BattlePersonageStats(
    int remainHealth,
    long normalDamageDealt,
    long normalAttackCount,
    long critDamageDealt,
    long critsCount,
    long damageBlocked,
    long blockCount,
    long damageDodged,
    long dodgesCount,
    long missesCount
) {
}
