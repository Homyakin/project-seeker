package ru.homyakin.seeker.character;

import org.springframework.stereotype.Service;

@Service
public class CharacterService {
    private final CharacterDao characterDao;

    public CharacterService(CharacterDao characterDao) {
        this.characterDao = characterDao;
    }

    public Character createCharacter() {
        final var id = characterDao.saveCharacter(1, 0);
        return characterDao.getById(id)
            .orElseThrow(() -> new IllegalStateException("Character must be present after create"));
    }
}
