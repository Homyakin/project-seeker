package ru.homyakin.seeker.game.personage.models;

import io.vavr.control.Either;
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

    public Characteristics multiply(Energy energy) {
        final var energyPercent = energy.percent();
        return new Characteristics(
            (int) (health * Math.max(0.5, energyPercent)),
            (int) (attack * Math.max(0.3, energyPercent)),
            (int) (defense * Math.max(0.3, energyPercent)),
            Math.max(1, Math.round(strength * energyPercent)),
            Math.max(1, Math.round(agility * energyPercent)),
            Math.max(1, Math.round(wisdom * energyPercent))
        );
    }

    public static Characteristics random() {
        int strength = 1;
        int agility = 1;
        int wisdom = 1;
        while (strength + agility + wisdom < MAX_LEVELING_POINTS) {
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
        return -8 / (advantage + 8) + 2;
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

    private static final int MAX_LEVELING_POINTS = 12;
    private static final double ADVANTAGE_MULTIPLIER = 2;
}
