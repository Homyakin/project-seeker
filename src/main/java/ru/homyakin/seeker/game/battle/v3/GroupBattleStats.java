package ru.homyakin.seeker.game.battle.v3;

public record GroupBattleStats(
    long remainHealth,
    long totalHealth,
    long normalDamageDealt,
    long normalAttackCount,
    long critDamageDealt,
    long critsCount,
    long damageBlocked,
    long blockCount,
    long damageDodged,
    long dodgesCount,
    long missesCount,
    int totalPersonages,
    int remainPersonages
) {
    public long damageDealtAndTaken() {
        return damageDealt() + damageTaken();
    }

    public long damageDealt() {
        return normalDamageDealt + critDamageDealt;
    }

    public long damageTaken() {
        return damageBlocked + damageDodged;
    }

    public static GroupBattleStats of(BattleStats stats) {
        return new GroupBattleStats(
            stats.remainHealth(),
            stats.totalHealth(),
            stats.normalDamageDealt(),
            stats.normalAttackCount(),
            stats.critDamageDealt(),
            stats.critsCount(),
            stats.damageBlocked(),
            stats.blocksCount(),
            stats.damageDodged(),
            stats.dodgesCount(),
            stats.missesCount(),
            stats.totalPersonages(),
            stats.remainPersonages()
        );
    }
}
