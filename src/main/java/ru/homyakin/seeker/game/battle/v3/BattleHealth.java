package ru.homyakin.seeker.game.battle.v3;

import ru.homyakin.seeker.game.personage.models.Characteristics;

public class BattleHealth implements Cloneable {
    private final int maxHealth;
    private int health;
    private int defense;

    public BattleHealth(Characteristics characteristics) {
        maxHealth = characteristics.health();
        health = characteristics.health();
        defense = 4 * characteristics.defense();
    }

    public void takeDamage(int damage) {
        int remainingDamage = damage;
        if (defense > 0) {
            if (damage > defense) {
                remainingDamage = damage - defense;
                defense = 0;
            } else {
                defense -= damage;
                remainingDamage = 0;
            }
        }
        if (remainingDamage > health) {
            health = 0;
        } else {
            health -= remainingDamage;
        }
    }

    public boolean isAlive() {
        return health > 0;
    }

    public int totalHealth() {
        return health + defense;
    }

    public int defense() {
        return defense;
    }

    public int remainingHealth() {
        return health;
    }

    public int maxHealth() {
        return maxHealth;
    }

    @Override
    @SuppressWarnings("super")
    public BattleHealth clone() {
        return new BattleHealth(maxHealth, health, defense);
    }

    private BattleHealth(int maxHealth, int health, int defense) {
        this.maxHealth = maxHealth;
        this.health = health;
        this.defense = defense;
    }
}
