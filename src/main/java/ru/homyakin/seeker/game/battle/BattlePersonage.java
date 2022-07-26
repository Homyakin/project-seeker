package ru.homyakin.seeker.game.battle;

import ru.homyakin.seeker.utils.RandomUtils;

public class BattlePersonage implements Comparable<BattlePersonage> {
    private final long id;
    private int health;
    private int damageDealt = 0;
    private int damageBlocked = 0;
    private final Characteristics characteristics;

    public BattlePersonage(
        long id,
        int health,
        int maxHealth,
        int attack,
        int defense,
        int strength,
        int agility,
        int wisdom
    ) {
        this.id = id;
        this.health = health;
        characteristics = new Characteristics(
            maxHealth,
            attack,
            defense,
            strength,
            agility,
            wisdom
        );
    }

    public long id() {
        return id;
    }

    public int damageBlocked() {
        return damageBlocked;
    }

    public int damageDealt() {
        return damageDealt;
    }

    public int damageDealtAndTaken() {
        return damageDealt + damageBlocked;
    }

    public int health() {
        return health;
    }

    public boolean isDead() {
        return health <= 0;
    }

    @Override
    public int compareTo(BattlePersonage o) {
        return 0;
    }

    public void dealDamageToPersonage(BattlePersonage enemy) {
        final var critBonus = critBonus(characteristics.wisdom() - enemy.characteristics.wisdom());
        final var attackBonus = 1 + (Math.max(characteristics.strength() - enemy.characteristics.strength(), 0)) * strengthMultiplier / 100;
        final var attackWithBonus = characteristics.attack() * attackBonus * critBonus;
        damageDealt += enemy.takeDamageAndReturnDealtDamage(attackWithBonus, this);
    }

    private int takeDamageAndReturnDealtDamage(double attack, BattlePersonage enemy) {
        damageBlocked += attack;
        final var agilityDiff = characteristics.agility() - enemy.characteristics.agility();
        if (isDodge(agilityDiff)) {
            return 0;
        }
        final var defenseBonus = 1 + (Math.max(agilityDiff, 0)) * agilityDefenseMultiplier / 100;
        final var finalDamage = (int) Math.max(attack - defenseBonus, enemy.minAttack());
        final int dealtDamage;
        if (health < finalDamage) {
            dealtDamage = health;
            health = 0;
        } else {
            dealtDamage = finalDamage;
            health -= finalDamage;
        }
        return dealtDamage;
    }

    private boolean isDodge(int agilityDiff) {
        var hitChance = 50 + agilityDiff * agilityHitChanceMultiplier;
        hitChance = Math.max(minHitChance, Math.min(hitChance, maxHitChance));
        final var chance = RandomUtils.getInInterval(1, 100);
        return chance <= hitChance;
    }

    private double critBonus(int wisdomDiff) {
        var critChance = 50 + wisdomDiff * wisdomCritChanceMultiplier;
        critChance = Math.max(minCritChance, Math.min(critChance, maxCritChance));
        final var chance = RandomUtils.getInInterval(1, 100);
        if (chance <= critChance) {
            return 1 + (Math.max(wisdomDiff, 0)) * wisdomCritMultiplier / 100;
        } else {
            return 1;
        }
    }

    private double minAttack() {
        return characteristics.attack() * minAttackPercent;
    }
    // TODO вынести в базу
    private static final int minHitChance = 30;
    private static final int maxHitChance = 30;
    private static final int minCritChance = 10;
    private static final int maxCritChance = 90;
    private static final double minAttackPercent = 0.5;
    private static final double strengthMultiplier = 2;
    private static final double agilityDefenseMultiplier = 1.7;
    private static final double agilityHitChanceMultiplier = 0.5;
    private static final double wisdomCritMultiplier = 1.5;
    private static final double wisdomCritChanceMultiplier = 0.5;
}

record Characteristics(
    int maxHealth,
    int attack,
    int defense,
    int strength,
    int agility,
    int wisdom
) {
}
