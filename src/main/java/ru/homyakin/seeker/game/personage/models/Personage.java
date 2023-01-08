package ru.homyakin.seeker.game.personage.models;

import io.vavr.control.Either;
import java.time.Duration;
import java.time.LocalDateTime;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.experience.ExperienceUtils;
import ru.homyakin.seeker.game.personage.PersonageDao;
import ru.homyakin.seeker.game.personage.models.errors.TooLongName;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Localization;
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

    public Personage checkHealthAndRegenIfNeed(PersonageDao personageDao) {
        final var maximumHealth = maxHealth();
        final var checkTime = TimeUtils.moscowTime();
        final int newHealth;
        if (health < maximumHealth) {
            final var minutesPass = Duration.between(lastHealthChange, checkTime).toMinutes();
            final double increaseHealth = ((double) maximumHealth) / 100 * minutesPass;
            if (health + increaseHealth >= maximumHealth) {
                newHealth = maximumHealth;
            } else {
                if (increaseHealth < 1) {
                    newHealth = health + 1;
                } else {
                    newHealth = health + (int) increaseHealth;
                }
            }
            personageDao.updateHealth(id, newHealth, checkTime);
            return copyWithHealthAndLastHealthChange(newHealth, checkTime);
        }
        return this;
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
}
