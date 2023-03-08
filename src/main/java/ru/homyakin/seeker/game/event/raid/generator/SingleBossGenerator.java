package ru.homyakin.seeker.game.event.raid.generator;

import java.util.Collections;
import java.util.List;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.personage.models.Characteristics;

public class SingleBossGenerator implements RaidBattleGenerator {
    @Override
    public List<BattlePersonage> generate(int personagesCount) {
        final var characteristics = Characteristics.createDefault();
        final var boss = new BattlePersonage(
            -1,
            new Characteristics(
                (int) (characteristics.health() * personagesCount),
                (int) (characteristics.attack() * Math.pow(personagesCount, 0.01)),
                (int) (characteristics.defense() * Math.pow(personagesCount, 0.01)),
                characteristics.strength(),
                characteristics.agility(),
                characteristics.wisdom()
            )
        );
        return Collections.singletonList(boss);
    }
}
