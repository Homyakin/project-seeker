package ru.homyakin.seeker.game.battle;

public class BattleStats {
    private long normalDamageDealt = 0L;
    private long normalAttackCount = 0L;
    private long critDamageDealt = 0L;
    private long critsCount = 0L;
    private long damageBlocked = 0L;
    private long blocksCount = 0L;
    private long damageDodged = 0L;
    private long dodgesCount = 0L;
    private long missesCount = 0L;

    void increaseDamageBlocked(long damageBlocked) {
        this.damageBlocked = this.damageBlocked + damageBlocked;
    }

    void incrementBlocksCount() {
        ++blocksCount;
    }

    void increaseDamageDodged(long damageDodged) {
        this.damageDodged = this.damageDodged + damageDodged;
    }

    void incrementDodgesCount() {
        ++dodgesCount;
    }

    void increaseNormalDamageDealt(long normalDamageDealt) {
        this.normalDamageDealt = this.normalDamageDealt + normalDamageDealt;
    }

    void incrementNormalAttackCount() {
        ++normalAttackCount;
    }

    void increaseCritDamageDealt(long critDamageDealt) {
        this.critDamageDealt = this.critDamageDealt + critDamageDealt;
    }

    void incrementCritsCount() {
        ++critsCount;
    }

    void incrementMissesCount() {
        ++missesCount;
    }

    public long normalDamageDealt() {
        return normalDamageDealt;
    }

    public long normalAttackCount() {
        return normalAttackCount;
    }

    public long critDamageDealt() {
        return critDamageDealt;
    }

    public long critsCount() {
        return critsCount;
    }

    public long damageBlocked() {
        return damageBlocked;
    }

    public long blocksCount() {
        return blocksCount;
    }

    public long damageDodged() {
        return damageDodged;
    }

    public long dodgesCount() {
        return dodgesCount;
    }

    public long missesCount() {
        return missesCount;
    }
}
