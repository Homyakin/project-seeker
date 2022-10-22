package ru.homyakin.seeker.game.personage.models;

import io.vavr.control.Either;
import ru.homyakin.seeker.game.experience.ExperienceUtils;
import ru.homyakin.seeker.game.personage.PersonageDao;
import ru.homyakin.seeker.game.personage.models.errors.TooLongName;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Localization;

public record Personage(
    long id,

    String name,
    int level,
    long currentExp
) {
    public Personage addExperience(long exp, PersonageDao personageDao) {
        final var newExp = currentExp + exp;
        var newLvl = level;
        if (newExp >= ExperienceUtils.getTotalExpToNextLevel(level)) {
            newLvl += 1;
        }
        final var personage = new Personage(
            id,
            name,
            newLvl,
            newExp
        );
        personageDao.update(personage);
        return personage;
    }

    public Either<TooLongName, Personage> changeName(String name, PersonageDao personageDao) {
        if (name.length() > MAX_NAME_LENGTH) {
            return Either.left(new TooLongName());
        }
        final var personage = new Personage(
            id,
            name,
            level,
            currentExp
        );
        personageDao.update(personage);
        return Either.right(personage);
    }

    public String toTopText() {
        return TextConstants.LEVEL_ICON + "%d %s: %d".formatted(level, name, currentExp);
    }

    public String toProfile(Language language) {
        return Localization
            .get(language)
            .profileTemplate()
            .formatted(name, level, currentExp, ExperienceUtils.getTotalExpToNextLevel(level));
    }

    public static final int MAX_NAME_LENGTH = 100;
}
