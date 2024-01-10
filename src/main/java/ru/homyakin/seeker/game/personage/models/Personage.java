package ru.homyakin.seeker.game.personage.models;

import io.vavr.control.Either;
import java.time.LocalDateTime;
import java.util.regex.Pattern;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.PersonageDao;
import ru.homyakin.seeker.game.personage.models.errors.EnergyStillSame;
import ru.homyakin.seeker.game.personage.models.errors.NameError;
import ru.homyakin.seeker.game.personage.models.errors.NotEnoughLevelingPoints;
import ru.homyakin.seeker.game.personage.models.errors.NotEnoughMoney;
import ru.homyakin.seeker.game.personage.models.errors.PersonageEventError;
import ru.homyakin.seeker.infrastructure.Icons;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.locale.personal.CharacteristicLocalization;
import ru.homyakin.seeker.utils.models.Success;

public record Personage(
    PersonageId id,
    String name,
    Money money,
    Characteristics characteristics,
    Energy energy
) {
    public Personage addMoney(Money money) {
        return new Personage(
            id,
            name,
            this.money.add(money),
            characteristics,
            energy
        );
    }

    public Either<NameError, Personage> changeName(String name, PersonageDao personageDao) {
        return validateName(name)
            .map(this::copyWithName)
            .peek(personageDao::update);
    }

    public static Either<NameError, String> validateName(String name) {
        if (name.length() < MIN_NAME_LENGTH || name.length() > MAX_NAME_LENGTH) {
            return Either.left(new NameError.InvalidLength(MIN_NAME_LENGTH, MAX_NAME_LENGTH));
        }
        if (!NAME_PATTERN.matcher(name).matches()) {
            return Either.left(new NameError.NotAllowedSymbols());
        }
        return Either.right(name);
    }

    public String shortProfile(Language language) {
        return CommonLocalization.shortProfile(language, this);
    }

    public String fullProfile(Language language) {
        final var profile = CommonLocalization.fullProfile(language, this);

        return characteristics.hasUnspentLevelingPoints()
            ? CharacteristicLocalization.profileLevelUp(language) + "\n\n" + profile : profile;
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

    public Either<EnergyStillSame, Personage> regenEnergyIfNeed() {
        return energy
            .regenIfNeeded()
            .map(this::copyWithEnergy);
    }

    public Personage nullifyEnergy(LocalDateTime energyChangeTime) {
        return copyWithEnergy(Energy.createZero(energyChangeTime));
    }

    public String icon() {
        return Icons.PERSONAGE;
    }

    public String iconWithName() {
        return icon() + name;
    }

    public Either<NotEnoughMoney, Personage> resetStats() {
        if (money.lessThan(RESET_STATS_COST)) {
            return Either.left(new NotEnoughMoney(RESET_STATS_COST));
        }

        return Either.right(
            addMoney(RESET_STATS_COST.negative())
                .copyWithCharacteristics(characteristics.reset())
        );
    }

    public Either<PersonageEventError, Success> hasEnoughEnergyForEvent() {
        return energy.isEnoughForEvent()
            .mapLeft(PersonageEventError.NotEnoughEnergy::new);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Personage other) {
           return this.id == other.id;
        }
        return false;
    }

    public BattlePersonage toBattlePersonage() {
        return new BattlePersonage(
            id.value(),
            characteristics,
            this
        );
    }

    public BattlePersonage toBattlePersonageUsingEnergy() {
        return new BattlePersonage(
            id.value(),
            characteristics.multiply(energy),
            this
        );
    }

    public static Personage createDefault()                {
        return createDefault(TextConstants.DEFAULT_NAME);
    }

    public static Personage createDefault(String name) {
        return new Personage(
            PersonageId.from(0L),
            name,
            Money.from(10),
            Characteristics.createDefault(),
            Energy.createDefault()
        );
    }

    private static final int MIN_NAME_LENGTH = 3;
    private static final int MAX_NAME_LENGTH = 25;
    private static final String CYRILLIC = "а-яА-ЯёЁ";
    private static final String ENGLISH = "a-zA-Z";
    private static final String NUMBERS = "0-9";
    private static final String SPECIAL = "_\\-\\.#№ ";
    private static final Pattern NAME_PATTERN = Pattern.compile("[" + CYRILLIC + ENGLISH + NUMBERS + SPECIAL + "]+");
    public static final Money RESET_STATS_COST = new Money(50);

    private Personage copyWithName(String name) {
        return new Personage(
            id,
            name,
            money,
            characteristics,
            energy
        );
    }

    private Personage copyWithCharacteristics(Characteristics characteristics) {
        return new Personage(
            id,
            name,
            money,
            characteristics,
            energy
        );
    }

    private Personage copyWithEnergy(Energy energy) {
        return new Personage(
            id,
            name,
            money,
            characteristics,
            energy
        );
    }
}
