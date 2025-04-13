package ru.homyakin.seeker.game.battle;

/**
 * Используется для записи статистики персонажей и групп
 */
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
    // Ниже только для групп
    private long remainHealth = 0L;
    private long totalHealth = 0L;
    private int totalPersonages = 0;
    private int remainPersonages = 0;

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

    /**
     * Считается статистика группы. Передаётся статистика одного персонажа
     */
    public void add(BattleStats personageStats, BattleHealth health) {
        this.normalDamageDealt = this.normalDamageDealt + personageStats.normalDamageDealt();
        this.normalAttackCount = this.normalAttackCount + personageStats.normalAttackCount();
        this.critDamageDealt = this.critDamageDealt + personageStats.critDamageDealt();
        this.critsCount = this.critsCount + personageStats.critsCount();
        this.damageBlocked = this.damageBlocked + personageStats.damageBlocked();
        this.blocksCount = this.blocksCount + personageStats.blocksCount();
        this.damageDodged = this.damageDodged + personageStats.damageDodged();
        this.dodgesCount = this.dodgesCount + personageStats.dodgesCount();
        this.missesCount = this.missesCount + personageStats.missesCount();
        this.remainHealth = this.remainHealth + health.remainingHealth();
        this.totalHealth = this.totalHealth + health.maxHealth();
        this.totalPersonages++;
        if (health.isAlive()) {
            this.remainPersonages++;
        }
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

    public long remainHealth() {
        return remainHealth;
    }

    public long totalHealth() {
        return totalHealth;
    }

    public int totalPersonages() {
        return totalPersonages;
    }

    public int remainPersonages() {
        return remainPersonages;
    }
}
