package ru.homyakin.seeker.game.event.raid.generator;

import java.util.Collections;
import java.util.List;
import ru.homyakin.seeker.game.personage.models.Characteristics;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.utils.RandomUtils;

public class SingleBossGenerator implements RaidBattleGenerator {
    @Override
    public List<Personage> generate(int personagesCount) {
        final var characteristics = Characteristics.random();
        final int health = characteristics.health() * personagesCount;
        final var boss = new Personage(
            -1,
            null,
            null,
            new Characteristics(
                RandomUtils.getInInterval((int) (health * 0.9), (int) (health * 1.1)),
                (int) (characteristics.attack() * Math.pow(personagesCount, 0.01)),
                (int) (characteristics.defense() * Math.pow(personagesCount, 0.01)),
                characteristics.strength(),
                characteristics.agility(),
                characteristics.wisdom()
            ),
            null
        );
        return Collections.singletonList(boss);
    }
}
