package ru.homyakin.seeker.game.event.raid.generator;

import java.util.Collections;
import java.util.List;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.utils.RandomUtils;

public class SingleBossGenerator implements RaidBattleGenerator {
    @Override
    public List<BattlePersonage> generate() {
        final var boss = new BattlePersonage(
            0,
            RandomUtils.getInInterval(1000, 3000),
            RandomUtils.getInInterval(1000, 3000),
            RandomUtils.getInInterval(20, 100),
            RandomUtils.getInInterval(20, 100),
            RandomUtils.getInInterval(10, 20),
            RandomUtils.getInInterval(10, 20),
            RandomUtils.getInInterval(10, 20)
        );
        return Collections.singletonList(boss);
    }
}
