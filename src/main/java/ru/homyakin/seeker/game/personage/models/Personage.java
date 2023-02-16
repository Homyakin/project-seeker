package ru.homyakin.seeker.game.personage.models;

import io.vavr.control.Either;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.regex.Pattern;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.personage.PersonageDao;
import ru.homyakin.seeker.game.personage.models.errors.NameError;
import ru.homyakin.seeker.game.personage.models.errors.NotEnoughLevelingPoints;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.locale.personal.LevelingLocalization;
import ru.homyakin.seeker.utils.MathUtils;
import ru.homyakin.seeker.utils.TimeUtils;

public record Personage(
    long id,
    String name,
    Money money,
    int health,
    int attack,
    int defense,
    int strength,
    int agility,
    int wisdom,
    LocalDateTime lastHealthChange
) {
    public Personage changeHealth(
        int health,
        LocalDateTime lastHealthChange,
        PersonageDao personageDao
    ) {
        if (this.health != health) {
            final var personage = copyWithHealthAndLastHealthChange(health, lastHealthChange);
            personageDao.update(personage);
            return personage;
        }
        return this;
    }

    public Either<NameError, Personage> changeName(String name, PersonageDao personageDao) {
        if (name.length() < MIN_NAME_LENGTH || name.length() > MAX_NAME_LENGTH) {
            return Either.left(new NameError.InvalidLength(MIN_NAME_LENGTH, MAX_NAME_LENGTH));
        }
        if (!NAME_PATTERN.matcher(name).matches()) {
            return Either.left(new NameError.NotAllowedSymbols());
        }
        final var personage = copyWithName(name);
        personageDao.update(personage);
        return Either.right(personage);
    }

    public String shortProfile(Language language) {
        return CommonLocalization
            .profileTemplate(language, this) + "\n" + shortStats();
    }

    public String fullProfile(Language language) {
        final var profile = CommonLocalization
            .profileTemplate(language, this) + "\n" + shortStats();

        return hasUnspentLevelingPoints() ? LevelingLocalization.profileLevelUp(language) + "\n\n" + profile : profile;
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

    public boolean hasHealthLessThanPercent(double minimumHealthPercent) {
        return health < maxHealth() * minimumHealthPercent;
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
            Money.zero(),
            maxHealth(),
            10,
            5,
            5,
            5,
            5,
            TimeUtils.moscowTime()
        );
    }

    private static final int MIN_NAME_LENGTH = 3;
    private static final int MAX_NAME_LENGTH = 25;
    private static final String CYRILLIC = "а-яА-ЯёЁ";
    private static final String ENGLISH = "a-zA-Z";
    private static final String NUMBERS = "0-9";
    private static final String SPECIAL = "_\\-\\.#№: ";
    private static final Pattern NAME_PATTERN = Pattern.compile("[" + CYRILLIC + ENGLISH + NUMBERS + SPECIAL + "]+");

    private int maxLevelingPoints() {
        return 12;
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

    private static int maxHealth() {
        return 500;
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

    private Personage copyWithName(String name) {
        return new Personage(
            id,
            name,
            money,
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
            money,
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
            money,
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
