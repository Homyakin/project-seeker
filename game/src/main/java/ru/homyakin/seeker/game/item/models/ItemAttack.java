package ru.homyakin.seeker.game.item.models;

public record ItemAttack(
    AttackType attackType,
    int minRange,
    int maxRange,
    int attack
) {
    public ItemAttack(AttackType attackType, int range, int attack) {
        this(attackType, 1, range, attack);
    }

    public ItemAttack {
        if (attackType == null) {
            throw new IllegalArgumentException("Attack type must be specified");
        }
        if (minRange < 1 || maxRange < minRange) {
            throw new IllegalArgumentException(
                "Invalid attack range: [%d;%d]".formatted(minRange, maxRange)
            );
        }
        if (attack <= 0) {
            throw new IllegalArgumentException("Attack must be positive: " + attack);
        }
    }

    public boolean isAvailableAt(int distance) {
        return distance >= minRange && distance <= maxRange;
    }
}
