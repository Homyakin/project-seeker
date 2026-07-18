package ru.homyakin.seeker.game.battle;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
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
    long missesCount,
    @JsonProperty(required = false) int turnsCount,
    @JsonProperty(required = false) long skillDamageDealt,
    @JsonProperty(required = false) long skillDamageCount
) {
    public long damageDealtAndTaken() {
        return damageDealt() + damageTaken();
    }

    public long damageDealt() {
        return normalDamageDealt + critDamageDealt + skillDamageDealt;
    }

    public long damageTaken() {
        return damageBlocked + damageDodged;
    }

    @JsonIgnore
    public boolean isDead() {
        return remainHealth <= 0;
    }
}
