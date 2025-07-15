package ru.homyakin.seeker.game.battle.v4;

import ru.homyakin.seeker.utils.RandomUtils;

public class BattlePersonage implements Cloneable {
    private final long id;
    private int health;
    private int attack;
    private int speed;

    public BattlePersonage(long id, int health, int attack, int speed) {
        this.id = id;
        this.health = health;
        this.attack = attack;
        this.speed = speed;
    }

    public void dealDamageToPersonage(BattlePersonage enemy) {
        int damage = (int) (
            attack
                * randomizeAttackBonus()
                * critMultiplier()
        );
        enemy.takeDamage(damage);
    }

    private double randomizeAttackBonus() {
        return 1 + RandomUtils.getInInterval(-ATTACK_RND, ATTACK_RND) / 100.0;
    }

    private double critMultiplier() {
        if (RandomUtils.processChance(CRIT_CHANCE)) {
            return CRIT_MULTIPLIER;
        } else {
            return 1.0;
        }
    }

    public void takeDamage(int damage) {
        health -= damage;
    }

    public int attack() {
        return attack;
    }

    public long id() {
        return id;
    }

    public int speed() {
        return speed;
    }

    public boolean isDead() {
        return health <= 0;
    }

    @Override
    public BattlePersonage clone() {
        return new BattlePersonage(id, health, attack, speed);
    }

    private static final int CRIT_CHANCE = 10;
    private static final double CRIT_MULTIPLIER = 1.5;
    private static final int ATTACK_RND = 10;
}
