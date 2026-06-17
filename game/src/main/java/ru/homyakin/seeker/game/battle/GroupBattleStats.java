package ru.homyakin.seeker.game.battle;

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
}
