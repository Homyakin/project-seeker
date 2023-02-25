package ru.homyakin.seeker.game.personage.models;

import io.vavr.control.Either;
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
import ru.homyakin.seeker.utils.TimeUtils;

public record Personage(
    long id,
    String name,
    Money money,
    Characteristics characteristics,
    LocalDateTime lastHealthChange
) {
    public Personage addMoney(int value) {
        return new Personage(
            id,
            name,
            money.add(value),
            characteristics,
            lastHealthChange
        );
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
            .profileTemplate(language, this) + "\n" + characteristics.shortStats();
    }

    public String fullProfile(Language language) {
        final var profile = CommonLocalization
            .profileTemplate(language, this) + "\n" + characteristics.shortStats();

        return characteristics.hasUnspentLevelingPoints() ? LevelingLocalization.profileLevelUp(language) + "\n\n" + profile : profile;
    }

    public Either<NotEnoughLevelingPoints, Personage> incrementStrength() {
        return characteristics.incrementStrength().map(this::copyWithCharacteristics);
    }

    public Either<NotEnoughLevelingPoints, Personage> incrementAgility() {
        return characteristics.incrementAgility().map(this::copyWithCharacteristics);
    }

    public Either<NotEnoughLevelingPoints, Personage> incrementWisdom() {
        return characteristics.incrementWisdom().map(this::copyWithCharacteristics);
    }

    public Personage checkHealthAndRegenIfNeed(PersonageDao personageDao) {
        //TODO механика бодрости будет основа на этом, поэтому пока закомменчено
        /*
        final var maximumHealth = maxHealth();
        if (characteristics.health() >= maximumHealth) {
            return this;
        }
        final var minutesPass = Duration.between(lastHealthChange, TimeUtils.moscowTime()).toMinutes();
        final var increaseHealth = MathUtils.doubleToIntWithMinMaxValues(
            ((double) maximumHealth) / 100 * minutesPass
        );
        if (increaseHealth > 0) {
            final int newHealth = Math.min(characteristics.health() + increaseHealth, maximumHealth);
            final var personage = copyWithHealthAndLastHealthChange(newHealth, lastHealthChange.plusMinutes(minutesPass));
            personageDao.update(personage);
            return personage;
        }
         */
        return this;
    }

    public String iconWithName() {
        return TextConstants.PERSONAGE_ICON + name;
    }

    public BattlePersonage toBattlePersonage() {
        return new BattlePersonage(
            id,
            characteristics
        );
    }

    public static Personage createDefault() {
        //TODO магические числа
        return new Personage(
            0L,
            TextConstants.DEFAULT_NAME,
            Money.zero(),
            Characteristics.createDefault(),
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

    private static int maxHealth() {
        return 500;
    }

    private Personage copyWithName(String name) {
        return new Personage(
            id,
            name,
            money,
            characteristics,
            lastHealthChange
        );
    }

    private Personage copyWithCharacteristics(Characteristics characteristics) {
        return new Personage(
            id,
            name,
            money,
            characteristics,
            lastHealthChange
        );
    }
}
