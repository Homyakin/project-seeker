package ru.homyakin.seeker.game.battle.v4;

public record BattlePersonageStats(
    int initialHealth,
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
    public long damageDealt() {
        return normalDamageDealt + critDamageDealt;
    }
}
