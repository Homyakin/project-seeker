package ru.homyakin.seeker.game.battle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.homyakin.seeker.game.personage.models.Characteristics;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.utils.RandomUtils;
import ru.homyakin.seeker.utils.models.Pair;

public class BattlePersonage implements Cloneable {
    private static final Logger logger = LoggerFactory.getLogger(BattlePersonage.class);
    private final long id;
    private int health;
    private final BattleStats battleStats = new BattleStats();
    private final Characteristics characteristics;
    private final Personage personage;

    public BattlePersonage(
        long id,
        Characteristics characteristics,
        Personage personage
    ) {
        this.id = id;
        this.health = characteristics.health();
        this.characteristics = characteristics;
        this.personage = personage;
    }

    public long id() {
        return id;
    }

    public int health() {
        return health;
    }

    public boolean isDead() {
        return health <= 0;
    }

    public BattleStats battleStats() {
        return battleStats;
    }

    public Personage personage() {
        return personage;
    }

    public void dealDamageToPersonage(BattlePersonage enemy) {
        final var critMultiplier = critMultiplier(enemy);
        final var attack = (int) Math.max(
            this.characteristics.attack()
                * this.characteristics.advantage(enemy.characteristics)
                * this.attackBonus(enemy)
                * critMultiplier.second()
                - enemy.characteristics.defense(),
            this.characteristics.attack() * minAttack
        );
        if (enemy.dodge(this)) {
            enemy.battleStats.incrementDodgesCount();
            enemy.battleStats.increaseDamageDodged(attack);
            this.battleStats.incrementMissesCount();
            return;
        }

        if (critMultiplier.first()) {
            this.battleStats.increaseCritDamageDealt(attack);
            this.battleStats.incrementCritsCount();
        } else {
            this.battleStats.increaseNormalDamageDealt(attack);
            this.battleStats.incrementNormalAttackCount();
        }
        enemy.battleStats.increaseDamageBlocked(attack);
        enemy.battleStats.incrementBlocksCount();
        if (enemy.health() <= attack) {
            enemy.health = 0;
        } else {
            enemy.health -= attack;
        }
    }

    private boolean dodge(BattlePersonage enemy) {
        final var diff = this.characteristics.agility() - enemy.characteristics.agility();
        final var offsetX = -28;
        var chance = baseDodgeChance;
        if (diff > offsetX) {
            chance += Math.max(-1960.0 / (diff - offsetX) + 70, 0);
        }
        return RandomUtils.getInInterval(1, 100) <= chance;
    }

    private Pair<Boolean, Double> critMultiplier(BattlePersonage enemy) {
        final var diff = this.characteristics.wisdom() - enemy.characteristics.wisdom();
        if (!isCrit(diff)) {
            return Pair.of(false, 1.0);
        }
        var multiplier = baseCritMultiplier;
        final var offsetX = -6;
        if (diff > offsetX) {
            multiplier += Math.max(-9.0 / (diff - offsetX) + 1.5, 0);
        }
        return Pair.of(true, multiplier);
    }

    private boolean isCrit(int wisdomDiff) {
        var chance = baseCritChance;
        final var offsetX = -37.4;
        if (wisdomDiff > offsetX) {
            chance += Math.max(-2618.0 / (wisdomDiff - offsetX) + 70, 0);
        }
        return RandomUtils.getInInterval(1, 100) <= chance;
    }

    private double attackBonus(BattlePersonage enemy) {
        var bonus = 0.0;
        final var offsetX = -12;
        final var diff = this.characteristics.strength() - enemy.characteristics.strength();
        if (diff > offsetX) {
            bonus += Math.max(-1.44 / (diff - offsetX) + 0.12, 0);
        }
        final double randomAttack = RandomUtils.getInInterval(100 - attackDeviation, 100 + attackDeviation) / 100.0;
        return randomAttack + bonus;
    }

    public PersonageBattleResult toResult() {
        return new PersonageBattleResult(
            personage,
            new PersonageBattleStats(
                health,
                battleStats.normalDamageDealt(),
                battleStats.normalAttackCount(),
                battleStats.critDamageDealt(),
                battleStats.critsCount(),
                battleStats.damageBlocked(),
                battleStats.blocksCount(),
                battleStats.damageDodged(),
                battleStats.dodgesCount(),
                battleStats.missesCount(),
                characteristics
            )
        );
    }

    // TODO вынести в базу
    private static final double baseDodgeChance = 10;
    private static final double baseCritChance = 10;
    private static final double baseCritMultiplier = 2;
    private static final int attackDeviation = 10;
    private static final double minAttack = 0.3;

    @Override
    @SuppressWarnings("super")
    public BattlePersonage clone() {
        return new BattlePersonage(
            id,
            health,
            characteristics.clone(),
            personage
        );
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof BattlePersonage battlePersonage) {
            return battlePersonage.id == this.id;
        }
        return false;
    }

    private BattlePersonage(
        long id,
        int health,
        Characteristics characteristics,
        Personage personage
    ) {
        this.id = id;
        this.health = health;
        this.characteristics = characteristics;
        this.personage = personage;
    }
}
