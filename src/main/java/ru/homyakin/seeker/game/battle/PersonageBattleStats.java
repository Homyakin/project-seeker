package ru.homyakin.seeker.game.battle;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ru.homyakin.seeker.game.personage.models.Characteristics;

public record PersonageBattleStats(
    int remainHealth,
    long normalDamageDealt,
    long normalAttackCount,
    long critDamageDealt,
    long critsCount,
    long damageBlocked,
    long blockCount,
    long damageDodged,
    long dodgesCount,
    long missesCount,
    Characteristics characteristics
) {
    public long damageDealtAndTaken() {
        return normalDamageDealt + critDamageDealt + damageBlocked + damageDodged;
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
