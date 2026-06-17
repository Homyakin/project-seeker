package ru.homyakin.seeker.game.battle;

import ru.homyakin.seeker.game.item.models.AttackType;

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
