package ru.homyakin.seeker.game.battle;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record BattlePersonageStats(
    int initialHealth,
    int remainHealth,
    int attack,
    int defense,
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
    public long damageDealtAndTaken() {
        return damageDealt() + damageTaken();
    }

    public long damageDealt() {
        return normalDamageDealt + critDamageDealt;
    }

    public long damageTaken() {
        return damageBlocked + damageDodged;
    }

    @JsonIgnore
    public boolean isDead() {
        return remainHealth <= 0;
    }
}
