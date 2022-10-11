package ru.homyakin.seeker.game.personage;

import org.springframework.stereotype.Service;

@Service
public class PersonageService {
    private final PersonageDao personageDao;

    public PersonageService(PersonageDao personageDao) {
        this.personageDao = personageDao;
    }

    public Personage createPersonage() {
        final var id = personageDao.save(1, 0);
        return personageDao.getById(id)
            .orElseThrow(() -> new IllegalStateException("Personage must be present after create"));
    }
}
