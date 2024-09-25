package ru.homyakin.seeker.game.personage.models;

import io.vavr.control.Either;
import ru.homyakin.seeker.game.effect.Effect;
import ru.homyakin.seeker.game.effect.EffectCharacteristic;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffect;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffects;
import ru.homyakin.seeker.game.personage.models.errors.NotEnoughLevelingPoints;
import ru.homyakin.seeker.utils.RandomUtils;

public record Characteristics(
    int health,
    int attack,
    int defense,
    int strength,
    int agility,
    int wisdom
) implements Cloneable {
    public static Characteristics createDefault() {
        return new Characteristics(500, 50, 20, 5, 5, 5);
    }

    public Characteristics reset() {
        return new Characteristics(health, attack, defense, 1, 1, 1);
    }

    public Characteristics add(Characteristics other) {
        return new Characteristics(
            health + other.health,
            attack + other.attack,
            defense + other.defense,
            strength + other.strength,
            agility + other.agility,
            wisdom + other.wisdom
        );
    }

    public static Characteristics random() {
        int strength = 1;
        int agility = 1;
        int wisdom = 1;
        for (int i = 0; i < MAX_LEVELING_POINTS; ++i) {
            final var random = RandomUtils.getInInterval(1, 3);
            switch (random) {
                case 1 -> ++strength;
                case 2 -> ++agility;
                default -> ++wisdom;
            }
        }
        return new Characteristics(500, 50, 20, strength, agility, wisdom);
    }

    public Either<NotEnoughLevelingPoints, Characteristics> incrementStrength() {
        if (!hasUnspentLevelingPoints()) {
            return Either.left(new NotEnoughLevelingPoints());
        }
        return Either.right(copyWithStrength(strength + 1));
    }

    public Either<NotEnoughLevelingPoints, Characteristics> incrementAgility() {
        if (!hasUnspentLevelingPoints()) {
            return Either.left(new NotEnoughLevelingPoints());
        }
        return Either.right(copyWithAgility(agility + 1));
    }

    public Either<NotEnoughLevelingPoints, Characteristics> incrementWisdom() {
        if (!hasUnspentLevelingPoints()) {
            return Either.left(new NotEnoughLevelingPoints());
        }
        return Either.right(copyWithWisdom(wisdom + 1));
    }

    public boolean hasUnspentLevelingPoints() {
        return levelingPointsSpentOnStrength()
            + levelingPointsSpentOnAgility()
            + levelingPointsSpentOnWisdom()
            < MAX_LEVELING_POINTS;
    }

    public double advantage(Characteristics other) {
        final var strength1 = Math.max(this.strength - other.agility / ADVANTAGE_MULTIPLIER - other.wisdom, 1);
        final var agility1 = Math.max(this.agility - other.wisdom / ADVANTAGE_MULTIPLIER - other.strength, 1);
        final var wisdom1 = Math.max(this.wisdom - other.strength / ADVANTAGE_MULTIPLIER - other.agility, 1);
        final var strength2 = Math.max(other.strength - this.agility / ADVANTAGE_MULTIPLIER - this.wisdom, 1);
        final var agility2 = Math.max(other.agility - this.wisdom / ADVANTAGE_MULTIPLIER - this.strength, 1);
        final var wisdom2 = Math.max(other.wisdom - this.strength / ADVANTAGE_MULTIPLIER - this.agility, 1);
        final var advantage = (strength1 + agility1 + wisdom1) - (strength2 + agility2 + wisdom2);
        if (advantage <= 0) {
            return 1;
        }
        return 1 + (-4.125 / (advantage + 8.25) + 0.5);
    }

    public Characteristics copyWithHealth(int health) {
        return new Characteristics(
            health,
            attack,
            defense,
            strength,
            agility,
            wisdom
        );
    }

    public Characteristics apply(PersonageEffects effects) {
        return new EditableCharacteristics(this).apply(effects).toFinal();
    }

    @Override
    public Characteristics clone() {
        try {
            return (Characteristics) super.clone();
        } catch (CloneNotSupportedException e) {
            //Не может быть в record
            throw new RuntimeException(e);
        }
    }

    private int levelingPointsSpentOnStrength() {
        return strength - 1;
    }

    private int levelingPointsSpentOnAgility() {
        return agility - 1;
    }

    private int levelingPointsSpentOnWisdom() {
        return wisdom - 1;
    }

    private Characteristics copyWithStrength(int strength) {
        return new Characteristics(
            health,
            attack,
            defense,
            strength,
            agility,
            wisdom
        );
    }

    private Characteristics copyWithAgility(int agility) {
        return new Characteristics(
            health,
            attack,
            defense,
            strength,
            agility,
            wisdom
        );
    }

    private Characteristics copyWithWisdom(int wisdom) {
        return new Characteristics(
            health,
            attack,
            defense,
            strength,
            agility,
            wisdom
        );
    }

    private static class EditableCharacteristics {
        private int health;
        private int attack;
        private int defense;
        private int strength;
        private int agility;
        private int wisdom;

        public EditableCharacteristics(Characteristics characteristics) {
            health = characteristics.health();
            attack = characteristics.attack();
            defense = characteristics.defense();
            strength = characteristics.strength();
            agility = characteristics.agility();
            wisdom = characteristics.wisdom();
        }

        public EditableCharacteristics apply(PersonageEffects effects) {
            effects.effects().values().stream().map(PersonageEffect::effect).forEach(this::apply);
            return this;
        }

        private void apply(Effect effect) {
            switch (effect) {
                case Effect.Add add -> {
                    switch (add.characteristic()) {
                        case HEALTH -> health = health + add.value();
                        case ATTACK -> attack = attack + add.value();
                        case STRENGTH -> strength = strength + add.value();
                        case AGILITY -> agility = agility + add.value();
                        case WISDOM -> wisdom = wisdom + add.value();
                    }
                }
                case Effect.Multiplier multiplier -> {
                    final var value = 1 + multiplier.percent() / 100.0;
                    multiplyCharacteristic(value, multiplier.characteristic());
                }
                case Effect.MinusMultiplier multiplier -> {
                    final var value = 1 - multiplier.percent() / 100.0;
                    multiplyCharacteristic(value, multiplier.characteristic());
                }
            }
        }

        private void multiplyCharacteristic(double value, EffectCharacteristic characteristic) {
            switch (characteristic) {
                case HEALTH -> health = (int) (health * value);
                case ATTACK -> attack = (int) (attack * value);
                case STRENGTH -> strength = (int) (strength * value);
                case AGILITY -> agility = (int) (agility * value);
                case WISDOM -> wisdom = (int) (wisdom * value);
            }
        }

        public Characteristics toFinal() {
            return new Characteristics(
                health,
                attack,
                defense,
                strength,
                agility,
                wisdom
            );
        }
    }

    private static final int MAX_LEVELING_POINTS = 12;
    private static final double ADVANTAGE_MULTIPLIER = 2;
    public static final Characteristics ZERO = new Characteristics(0, 0, 0, 0, 0, 0);
}
