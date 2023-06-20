package ru.homyakin.seeker.game.battle;

public class BattleStats {
    private long damageDealt = 0L;
    private long damageTaken = 0L;
    private long damageDodged = 0L;
    private long dodgesCount = 0L;

    void increaseDamageDealt(long damageDealt) {
        this.damageDealt = this.damageDealt + damageDealt;
    }

    void increaseDamageTaken(long damageTaken) {
        this.damageTaken = this.damageTaken + damageTaken;
    }

    void increaseDamageDodged(long damageDodged) {
        this.damageDodged = this.damageDodged + damageDodged;
    }

    void incrementDodgesCount() {
        ++dodgesCount;
    }

    public long damageDealtAndBlocked() {
        return damageTaken + damageDodged + damageDealt;
    }

    public long damageDealt() {
        return damageDealt;
    }

    public long damageTaken() {
        return damageTaken;
    }

    public long dodgesCount() {
        return dodgesCount;
    }
}
