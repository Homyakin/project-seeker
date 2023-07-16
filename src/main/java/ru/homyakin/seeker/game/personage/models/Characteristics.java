package ru.homyakin.seeker.game.personage.models;

import io.vavr.control.Either;
import ru.homyakin.seeker.game.personage.models.errors.NotEnoughLevelingPoints;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.utils.RandomUtils;

public record Characteristics(

    int health,
    int attack,
    int defense,
    int strength,
    int agility,
    int wisdom
) {
    public static Characteristics createDefault() {
        return new Characteristics(500, 50, 20, 5, 5, 5);
    }

    public Characteristics reset() {
        return new Characteristics(health, attack, defense, 1, 1, 1);
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

    public String shortStats() {
        return
            """
            %s%d%s%d%s%d
            %s%d%s%d%s%d
            """.formatted(
                TextConstants.HEALTH_ICON, health,
                TextConstants.ATTACK_ICON, attack,
                TextConstants.DEFENSE_ICON, defense,
                TextConstants.STRENGTH_ICON, strength,
                TextConstants.AGILITY_ICON, agility,
                TextConstants.WISDOM_ICON, wisdom
            );
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
}
