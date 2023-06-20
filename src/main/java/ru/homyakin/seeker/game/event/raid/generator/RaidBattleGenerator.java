package ru.homyakin.seeker.game.event.raid.generator;

import java.util.List;
import ru.homyakin.seeker.game.personage.models.Personage;

public interface RaidBattleGenerator {
    List<Personage> generate(int personagesCount);
}
