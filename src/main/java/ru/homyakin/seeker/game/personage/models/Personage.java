package ru.homyakin.seeker.game.personage.models;

import io.vavr.control.Either;
import java.time.Duration;
import java.time.LocalDateTime;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.experience.ExperienceUtils;
import ru.homyakin.seeker.game.personage.PersonageDao;
import ru.homyakin.seeker.game.personage.models.errors.NotEnoughLevelingPoints;
import ru.homyakin.seeker.game.personage.models.errors.TooLongName;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Localization;
import ru.homyakin.seeker.utils.MathUtils;
import ru.homyakin.seeker.utils.TimeUtils;

public record Personage(
    long id,

    String name,
    int level,
    long currentExp,
    int health,
    int attack,
    int defense,
    int strength,
    int agility,
    int wisdom,
    LocalDateTime lastHealthChange
) {
    public Personage addExperienceAndChangeHealth(
        long exp,
        int health,
        LocalDateTime lastHealthChange,
        PersonageDao personageDao
    ) {
        final var newExp = currentExp + exp;
        var newLevel = ExperienceUtils.getCurrentLevelForExp(newExp);
        var personage = copyWithLevelAndExp(newLevel, newExp);
        if (personage.health() != health) {
            personage = personage.copyWithHealthAndLastHealthChange(health, lastHealthChange);
        }
        personageDao.update(personage);
        return personage;
    }

    public Either<TooLongName, Personage> changeName(String name, PersonageDao personageDao) {
        if (name.length() > MAX_NAME_LENGTH) {
            return Either.left(new TooLongName());
        }
        final var personage = copyWithName(name);
        personageDao.update(personage);
        return Either.right(personage);
    }

    public String toTopText() {
        return TextConstants.LEVEL_ICON + "%d %s: %d".formatted(level, name, currentExp);
    }

    public String shortProfile(Language language) {
        return Localization
            .get(language)
            .profileTemplate()
            .formatted(name, level, currentExp, ExperienceUtils.getTotalExpToNextLevel(level)) + shortStats();
    }

    public String fullProfile(Language language) {
        final var profile = Localization
            .get(language)
            .profileTemplate()
            .formatted(name, level, currentExp, ExperienceUtils.getTotalExpToNextLevel(level)) + shortStats();

        return hasUnspentLevelingPoints() ? Localization.get(language).profileLevelUp() + "\n\n" + profile : profile;
    }

    public Personage checkHealthAndRegenIfNeed(PersonageDao personageDao) {
        final var maximumHealth = maxHealth();
        if (health >= maximumHealth) {
            return this;
        }
        final var minutesPass = Duration.between(lastHealthChange, TimeUtils.moscowTime()).toMinutes();
        final var increaseHealth = MathUtils.doubleToIntWithMinMaxValues(
            ((double) maximumHealth) / 100 * minutesPass
        );
        if (increaseHealth > 0) {
            final int newHealth = Math.min(health + increaseHealth, maximumHealth);
            final var personage = copyWithHealthAndLastHealthChange(newHealth, lastHealthChange.plusMinutes(minutesPass));
            personageDao.update(personage);
            return personage;
        }
        return this;
    }

    public Either<NotEnoughLevelingPoints, Personage> incrementStrength(PersonageDao personageDao) {
        if (!hasUnspentLevelingPoints()) {
            return Either.left(new NotEnoughLevelingPoints());
        }
        final var personage = copyWithCharacteristics(strength + 1, agility, wisdom);
        personageDao.update(personage);
        return Either.right(personage);
    }

    public Either<NotEnoughLevelingPoints, Personage> incrementAgility(PersonageDao personageDao) {
        if (!hasUnspentLevelingPoints()) {
            return Either.left(new NotEnoughLevelingPoints());
        }
        final var personage = copyWithCharacteristics(strength, agility + 1, wisdom);
        personageDao.update(personage);
        return Either.right(personage);
    }

    public Either<NotEnoughLevelingPoints, Personage> incrementWisdom(PersonageDao personageDao) {
        if (!hasUnspentLevelingPoints()) {
            return Either.left(new NotEnoughLevelingPoints());
        }
        final var personage = copyWithCharacteristics(strength, agility, wisdom + 1);
        personageDao.update(personage);
        return Either.right(personage);
    }

    public boolean hasUnspentLevelingPoints() {
        return levelingPointsSpentOnStrength()
            + levelingPointsSpentOnAgility()
            + levelingPointsSpentOnWisdom()
            < maxLevelingPoints();
    }


    public BattlePersonage toBattlePersonage() {
        return new BattlePersonage(
            id,
            health,
            maxHealth(),
            attack,
            defense,
            strength,
            agility,
            wisdom
        );
    }

    public static Personage createDefault() {
        //TODO магические числа
        return new Personage(
            0L,
            TextConstants.DEFAULT_NAME,
            1,
            0,
            100,
            10,
            5,
            1,
            1,
            1,
            TimeUtils.moscowTime()
        );
    }

    public static final int MAX_NAME_LENGTH = 100;

    private int maxLevelingPoints() {
        return level - 1;
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

    private int maxHealth() {
        return 50 + 50 * level;
    }

    private String shortStats() {
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

    private Personage copyWithLevelAndExp(int newLevel, long newExp) {
        return new Personage(
            id,
            name,
            newLevel,
            newExp,
            health,
            attack,
            defense,
            strength,
            agility,
            wisdom,
            lastHealthChange
        );
    }

    private Personage copyWithName(String name) {
        return new Personage(
            id,
            name,
            level,
            currentExp,
            health,
            attack,
            defense,
            strength,
            agility,
            wisdom,
            lastHealthChange
        );
    }

    private Personage copyWithHealthAndLastHealthChange(int health, LocalDateTime lastHealthChange) {
        return new Personage(
            id,
            name,
            level,
            currentExp,
            health,
            attack,
            defense,
            strength,
            agility,
            wisdom,
            lastHealthChange
        );
    }

    private Personage copyWithCharacteristics(int strength, int agility, int wisdom) {
        return new Personage(
            id,
            name,
            level,
            currentExp,
            health,
            attack,
            defense,
            strength,
            agility,
            wisdom,
            lastHealthChange
        );
    }
}
