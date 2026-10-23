package ru.homyakin.seeker.game.battle;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Optional;
import ru.homyakin.seeker.game.personage.power.PowerDisplaySnapshot;

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
    @JsonProperty(required = false) long skillDamageCount,
    @JsonProperty(required = false) @JsonInclude(JsonInclude.Include.NON_ABSENT) Optional<PowerDisplaySnapshot> displayedPower
) {
    public BattlePersonageStats {
        displayedPower = displayedPower == null ? Optional.empty() : displayedPower;
    }

    public BattlePersonageStats(
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
        int turnsCount,
        long skillDamageDealt,
        long skillDamageCount
    ) {
        this(
            initialHealth, remainHealth, attack, defense, normalDamageDealt, normalAttackCount,
            critDamageDealt, critsCount, damageBlocked, blockCount, damageDodged, dodgesCount,
            missesCount, turnsCount, skillDamageDealt, skillDamageCount, Optional.empty()
        );
    }

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
