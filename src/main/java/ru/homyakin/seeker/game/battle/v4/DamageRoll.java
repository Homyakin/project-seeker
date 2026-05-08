package ru.homyakin.seeker.game.battle.v4;

import java.util.Map;

public record DamageRoll(Map<AttackType, Integer> attack, boolean crit) {
    public int amount() {
        var total = 0;
        for (final var value: attack.values()) {
            total += value;
        }
        return total;
    }
}
