package ru.homyakin.seeker.game.personage.model;

import ru.homyakin.seeker.game.personage.PersonageDao;

public record Personage(
    long id,
    int level,
    long currentExp
) {
    public Personage addExperience(long exp, PersonageDao personageDao) {
        final var personage = new Personage(
            id,
            level,
            currentExp + exp
        );
        personageDao.update(personage);
        return personage;
    }
}
