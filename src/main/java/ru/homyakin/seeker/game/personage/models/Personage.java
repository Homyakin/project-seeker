package ru.homyakin.seeker.game.personage.models;

import io.vavr.control.Either;
import ru.homyakin.seeker.game.battle.BattlePersonage;
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
    long currentExp,
    int health,
    int attack,
    int defense,
    int strength,
    int agility,
    int wisdom
) {
    public Personage addExperience(long exp, PersonageDao personageDao) {
        final var newExp = currentExp + exp;
        var newLvl = level;
        if (newExp >= ExperienceUtils.getTotalExpToNextLevel(level)) {
            newLvl += 1;
        }
        final var personage = copyWithLevelAndExp(newLvl, newExp);
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

    public String toProfile(Language language) {
        return Localization
            .get(language)
            .profileTemplate()
            .formatted(name, level, currentExp, ExperienceUtils.getTotalExpToNextLevel(level)) + shortStats();
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
            1
        );
    }

    public static final int MAX_NAME_LENGTH = 100;

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
            wisdom
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
            wisdom
        );
    }
}
