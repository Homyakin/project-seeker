package ru.homyakin.seeker.game.battle.v3.two_team;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.homyakin.seeker.game.personage.models.Characteristics;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.utils.RandomUtils;
import ru.homyakin.seeker.utils.models.Pair;

public class BattlePersonage implements Cloneable {
    private static final Logger logger = LoggerFactory.getLogger(BattlePersonage.class);
    private final long id;
    private final BattleStats battleStats = new BattleStats();
    /**
     * АХТУНГ!!! При расчете мощи персонажа, характеристику учитываются без эффектов
     * Это нужно, чтобы повысить винрейт при использовании эффектов
     */
    private final double power;
    private BattleHealth health;
    private Characteristics characteristics;
    private final Personage personage;

    public BattlePersonage(
        long id,
        Characteristics characteristics,
        Personage personage
    ) {
        this.id = id;
        this.characteristics = characteristics;
        this.health = new BattleHealth(this.characteristics);
        this.power = calculatePower();
        if (personage != null) { // TODO это костылище, когда генерируем рейды, персонажа нет
            this.characteristics = this.characteristics.apply(personage.effects());
            this.health = new BattleHealth(this.characteristics);
        }
        this.personage = personage;

    }

    public long id() {
        return id;
    }

    public boolean isDead() {
        return !health.isAlive();
    }

    public void dealDamageToPersonage(BattlePersonage enemy) {
        final var critMultiplier = critMultiplier(enemy);
        final var attack = (int) (
            this.characteristics.attack()
                * critMultiplier.second()
                * randomizeAttackBonus()
        );
        if (enemy.isDodge(this)) {
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
        enemy.health.takeDamage(attack);
    }

    /**
     * Считаем абстрактную мощь персонажа
     */
    private double calculatePower() {
        final var critChance = calculateCritChance();
        final var dodgeChance = calculateDodgeChance();
        // Описание формулы
        // Мощность это ЗДОРОВЬЕ * АТАКУ
        // атака складывается из атаки и среднего крита
        // (1 / (1 - dodgeChance)) - атака противника умножается на (1 - dodgeChance), следовательно при переносе будет деление
        return health.totalHealth()
            * (1 - (critChance - baseCritMultiplier * critChance) / 100)
            * characteristics.attack()
            * (1 / (1 - dodgeChance / 100));
    }

    public double power() {
        return power;
    }

    public double calculateHealthForTargetPower(double targetPower) {
        final var critChance = calculateCritChance();
        final var dodgeChance = calculateDodgeChance();
        return targetPower
            / (1 - (critChance - baseCritMultiplier * critChance) / 100)
            / characteristics.attack()
            / (1 / (1 - dodgeChance / 100))
            - health.defense();
    }

    private boolean isDodge(BattlePersonage enemy) {
        return RandomUtils.getInInterval(1, 100) <= calculateDodgeChance();
    }

    private double calculateDodgeChance() {
        return baseDodgeChance;
    }

    private Pair<Boolean, Double> critMultiplier(BattlePersonage enemy) {
        final var isCrit = RandomUtils.getInInterval(1, 100) <= calculateCritChance();
        if (!isCrit) {
            return Pair.of(false, 1.0);
        }
        return Pair.of(true, baseCritMultiplier);
    }

    private double calculateCritChance() {
        return baseCritChance;
    }

    private double randomizeAttackBonus() {
        return RandomUtils.getInInterval(100 - attackDeviation, 100 + attackDeviation) / 100.0;
    }

    public PersonageBattleResult toResult() {
        return new PersonageBattleResult(
            personage,
            new PersonageBattleStats(
                health.remainingHealth(),
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

    public BattleStats stats() {
        return battleStats;
    }

    public BattleHealth health() {
        return health;
    }

    public Personage personage() {
        return personage;
    }

    // TODO вынести в базу
    private static final double baseDodgeChance = 5;
    private static final double baseCritChance = 5;
    private static final double baseCritMultiplier = 2;
    private static final int attackDeviation = 10;

    @Override
    @SuppressWarnings("super")
    public BattlePersonage clone() {
        return new BattlePersonage(
            id,
            health,
            power,
            characteristics,
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
        BattleHealth health,
        double power,
        Characteristics characteristics,
        Personage personage
    ) {
        this.id = id;
        this.health = health.clone();
        this.power = power;
        this.characteristics = characteristics.clone();
        this.personage = personage;
    }
}
