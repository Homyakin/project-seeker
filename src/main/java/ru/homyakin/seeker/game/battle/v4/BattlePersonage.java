package ru.homyakin.seeker.game.battle.v4;

public class BattlePersonage implements Cloneable {
    private final long id;
    private int health;
    private int attack;

    public BattlePersonage(long id, int health, int attack) {
        this.id = id;
        this.health = health;
        this.attack = attack;
    }

    public void dealDamageToPersonage(BattlePersonage enemy) {
        enemy.takeDamage(attack);
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

    public boolean isDead() {
        return health <= 0;
    }

    @Override
    public BattlePersonage clone() {
        return new BattlePersonage(id, health, attack);
    }
}
