package ru.homyakin.seeker.game.battle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.homyakin.seeker.game.personage.models.Characteristics;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.utils.RandomUtils;

public class BattlePersonage implements Cloneable {
    private static final Logger logger = LoggerFactory.getLogger(BattlePersonage.class);
    private final long id;
    private int health;
    private final BattleStats battleStats = new BattleStats();
    private final BattleCharacteristics characteristics;
    private final Personage personage;

    public BattlePersonage(
        long id,
        Characteristics characteristics,
        Personage personage
    ) {
        this.id = id;
        this.health = characteristics.health();
        this.characteristics = BattleCharacteristics.from(characteristics);
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

    public String statsText(Language language) {
        return CommonLocalization.personageBattleResult(language, this);
    }

    public void dealDamageToPersonage(BattlePersonage enemy) {
        final var attack = (int) Math.max(
            this.characteristics.attack
                * this.characteristics.advantage(enemy.characteristics)
                * this.attackBonus(enemy)
                * this.critMultiplier(enemy)
                - enemy.characteristics.defense,
            this.characteristics.attack * minAttack
        );
        if (enemy.dodge(this)) {
            enemy.battleStats.incrementDodgesCount();
            enemy.battleStats.increaseDamageDodged(attack);
            return;
        }
        enemy.battleStats.increaseDamageTaken(attack);
        if (enemy.health() <= attack) {
            this.battleStats.increaseDamageDealt(enemy.health());
            enemy.health = 0;
        } else {
            this.battleStats.increaseDamageDealt(attack);
            enemy.health -= attack;
        }
    }

    private boolean dodge(BattlePersonage enemy) {
        final var diff = this.characteristics.agility - enemy.characteristics.agility;
        final var offsetX = -28;
        var chance = baseDodgeChance;
        if (diff > offsetX) {
            chance += Math.max(-1960.0 / (diff - offsetX) + 70, 0);
        }
        return RandomUtils.getInInterval(1, 100) <= chance;
    }

    private double critMultiplier(BattlePersonage enemy) {
        final var diff = this.characteristics.wisdom - enemy.characteristics.wisdom;
        if (!isCrit(diff)) {
            return 1;
        }
        var multiplier = baseCritMultiplier;
        final var offsetX = -6;
        if (diff > offsetX) {
            multiplier += Math.max(-9.0 / (diff - offsetX) + 1.5, 0);
        }
        return multiplier;
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
        final var diff = this.characteristics.strength - enemy.characteristics.strength;
        if (diff > offsetX) {
            bonus += Math.max(-1.44 / (diff - offsetX) + 0.12, 0);
        }
        final double randomAttack = RandomUtils.getInInterval(100 - attackDeviation, 100 + attackDeviation) / 100.0;
        return randomAttack + bonus;
    }

    // TODO вынести в базу
    private static final double baseDodgeChance = 10;
    private static final double advantageMultiplier = 2;
    private static final double baseCritChance = 10;
    private static final double baseCritMultiplier = 2;
    private static final int attackDeviation = 10;
    private static final double minAttack = 0.3;

    record BattleCharacteristics(
        int attack,
        int defense,
        int strength,
        int agility,
        int wisdom
    ) implements Cloneable {
        public static BattleCharacteristics from(Characteristics characteristics) {
            return new BattleCharacteristics(
                characteristics.attack(),
                characteristics.defense(),
                characteristics.strength(),
                characteristics.agility(),
                characteristics.wisdom()
            );
        }

        @Override
        public BattleCharacteristics clone() {
            try {
                return (BattleCharacteristics) super.clone();
            } catch (CloneNotSupportedException e) {
                //Не может быть в record
                throw new RuntimeException(e);
            }
        }

        private double advantage(BattleCharacteristics other) {
            final var strength1 = Math.max(this.strength - other.agility / advantageMultiplier - other.wisdom, 1);
            final var agility1 = Math.max(this.agility - other.wisdom / advantageMultiplier - other.strength, 1);
            final var wisdom1 = Math.max(this.wisdom - other.strength / advantageMultiplier - other.agility, 1);
            final var strength2 = Math.max(other.strength - this.agility / advantageMultiplier - this.wisdom, 1);
            final var agility2 = Math.max(other.agility - this.wisdom / advantageMultiplier - this.strength, 1);
            final var wisdom2 = Math.max(other.wisdom - this.strength / advantageMultiplier - this.agility, 1);
            final var advantage = (strength1 + agility1 + wisdom1) - (strength2 + agility2 + wisdom2);
            if (advantage <= 0) {
                return 1;
            }
            return -8 / (advantage + 8) + 2;
        }
    }

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

    private BattlePersonage(
        long id,
        int health,
        BattleCharacteristics battleCharacteristics,
        Personage personage
    ) {
        this.id = id;
        this.health = health;
        this.characteristics = battleCharacteristics;
        this.personage = personage;
    }
}
