package ru.homyakin.seeker.game.season.action;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.season.entity.SeasonNumber;
import ru.homyakin.seeker.game.season.entity.SeasonStorage;

@Component
public class SeasonService {
    private final SeasonStorage seasonStorage;

    public SeasonService(SeasonStorage seasonStorage) {
        this.seasonStorage = seasonStorage;
    }

    public SeasonNumber currentSeason() {
        return seasonStorage.currentSeason();
    }
}
