package ru.homyakin.seeker.game.event.raid.generator;

import ru.homyakin.seeker.game.battle.v3.two_team.BattlePersonage;
import ru.homyakin.seeker.game.personage.models.Characteristics;
import java.util.Collections;
import java.util.List;

public class SingleBossGenerator implements RaidBattlePersonageGenerator {
    @Override
    public List<BattlePersonage> generate(int personagesCount, double powerMultiplier) {
        // 32000 - примерная мощь базового персонажа
        // 0.0832 - крайне магическая константа полученная методом перебора
        final var totalPower = personagesCount * 32000 * (powerMultiplier + 0.0832);
        final var tempCharacteristics = Characteristics.random();

        final var tempPersonage = new BattlePersonage(
            -1,
            tempCharacteristics,
            null
        );

        final var health = tempPersonage.calculateHealthForTargetPower(totalPower);
        final var characteristics = tempCharacteristics.copyWithHealth((int) health);
        final var boss = new BattlePersonage(-1, characteristics, null);
        return Collections.singletonList(boss);
    }
}
