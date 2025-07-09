package ru.homyakin.seeker.game.battle.v3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.homyakin.seeker.game.personage.models.Characteristics;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.utils.MathUtils;
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
                * this.characteristics.advantage(enemy.characteristics)
                * this.randomizeAttackBonus(enemy.characteristics.strength())
                * critMultiplier.second()
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
     * Считаем абстрактную мощь персонажа относительно персонажа с характеристиками 5/5/5
     */
    private double calculatePower() {
        final var critChance = calculateCritChance(5);
        final var dodgeChance = calculateDodgeChance(5);
        final var attackBonus = 1 + calculateAttackBonus(5);
        final var advantage = this.characteristics.advantage(new Characteristics(0, 0, 0, 5, 5, 5));
        // Описание формулы
        // Мощность это ЗДОРОВЬЕ * АТАКУ
        // атака складывается из атаки, бонуса и среднего крита
        // (1 / (1 - dodgeChance)) - атака противника умножается на (1 - dodgeChance), следовательно при переносе будет деление
        return health.totalHealth()
            * (1 - (critChance - baseCritMultiplier * critChance) / 100)
            * advantage
            * attackBonus
            * characteristics.attack()
            * (1 / (1 - dodgeChance / 100));
    }

    public double power() {
        return power;
    }

    public double calculateHealthForTargetPower(double targetPower) {
        final var critChance = calculateCritChance(5);
        final var dodgeChance = calculateDodgeChance(5);
        final var attackBonus = 1 + calculateAttackBonus(5);
        final var advantage = this.characteristics.advantage(new Characteristics(0, 0, 0, 5, 5, 5));
        return targetPower
            / (1 - (critChance - baseCritMultiplier * critChance) / 100)
            / attackBonus
            / characteristics.attack()
            / (1 / (1 - dodgeChance / 100))
            / advantage
            - health.defense();
    }

    private boolean isDodge(BattlePersonage enemy) {
        return RandomUtils.getInInterval(1, 100) <= calculateDodgeChance(enemy.characteristics.agility());
    }

    /**
     * Примерные значения шансов при разнице ловкостей:
     * 0 -> 5
     * 5 -> 11.3
     * 12 -> 17.9
     */
    private double calculateDodgeChance(int enemyAgility) {
        return baseDodgeChance + MathUtils.calcOneDivideXFunc(
            this.characteristics.agility() - enemyAgility,
            -1707.69,
            -34.15,
            50
        );
    }

    private Pair<Boolean, Double> critMultiplier(BattlePersonage enemy) {
        final var isDodge = RandomUtils.getInInterval(1, 100) <= calculateCritChance(enemy.characteristics.wisdom());
        if (!isDodge) {
            return Pair.of(false, 1.0);
        }
        return Pair.of(true, baseCritMultiplier);
    }

    /**
     * Примерные значения шансов при разнице мудростей:
     * 0 -> 5
     * 5 -> 13.8
     * 12 -> 21.9
     */
    private double calculateCritChance(int enemyWisdom) {
        return baseCritChance + MathUtils.calcOneDivideXFunc(
            this.characteristics.wisdom() - enemyWisdom,
            -1164.7,
            -23.29,
            50
        );
    }

    /**
     * @return возвращает значение > 1
     */
    private double randomizeAttackBonus(int enemyStrength) {
        final var bonus = calculateAttackBonus(enemyStrength);
        final double randomAttack = RandomUtils.getInInterval(100 - attackDeviation, 100 + attackDeviation) / 100.0;
        return randomAttack + bonus;
    }

    /**
     * Примерные значения бонусов при разнице сил:
     * 0 -> 0
     * 5 -> 0.1
     * 12 -> 0.17(7)
     */
    private double calculateAttackBonus(int enemyStrength) {
        return MathUtils.calcOneDivideXFunc(
            this.characteristics.strength() - enemyStrength,
            -6.0,
            -15.0,
            0.4
        );
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
