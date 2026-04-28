package ru.homyakin.seeker.game.battle.v4;

import java.util.UUID;

import ru.homyakin.seeker.utils.RandomUtils;

public class BattlePersonage {
    private static final int TURN_INITIATIVE = 1000;

    private final int rangePercent = 10;
    private final double defenseCoef = 500;

    private UUID id = UUID.randomUUID();
    private int health;
    private int attack;
    private int defense;
    private int critChance;
    private int dodgeChance;
    private double critMultiplier;
    private int initiative;
    private int nextMove;

    public BattlePersonage(
        int health,
        int attack,
        int defense,
        int critChance,
        int dodgeChance,
        double critMultiplier,
        int initiative
    ) {
        this.health = health;
        this.attack = attack;
        this.defense = defense;
        this.critChance = critChance;
        this.dodgeChance = dodgeChance;
        this.critMultiplier = critMultiplier;
        this.initiative = initiative;
        this.nextMove = RandomUtils.getInInterval(0, TURN_INITIATIVE / 2);
    }

    public void takeDamage(int damage) {
        if (RandomUtils.processChance(dodgeChance)) {
            return;
        }
        this.health = health - (int) (damage * defenseReduce());
    }

    public boolean isAlive() {
        return health > 0;
    }

    public boolean tick() {
        nextMove += initiative;
        if (nextMove >= TURN_INITIATIVE) {
            nextMove -= TURN_INITIATIVE;
            return true;
        }
        return false;
    }

    public UUID id() {
        return id;
    }

    public int getDamage() {
        if (RandomUtils.processChance(critChance)) {
            return RandomUtils.getInPercentRange((int) (attack * critMultiplier), rangePercent);
        }
        return RandomUtils.getInPercentRange(attack, rangePercent);
    }

    public double power() {
        final var critProbability = critChance / 100.0;
        final var effectiveDamage = attack * (1 + critProbability * (critMultiplier - 1));
        final var dodgeProbability = dodgeChance / 100.0;
        return health * effectiveDamage / defenseReduce() / (1 - dodgeProbability) * ((double) initiative / TURN_INITIATIVE);
    }

    private double defenseReduce() {
        return (1 - (defense / (defense + defenseCoef)));
    }
}
