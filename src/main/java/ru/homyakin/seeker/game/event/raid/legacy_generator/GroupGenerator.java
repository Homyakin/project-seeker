package ru.homyakin.seeker.game.event.raid.legacy_generator;

import java.util.ArrayList;
import java.util.List;
import ru.homyakin.seeker.game.battle.v3.two_team.BattlePersonage;
import ru.homyakin.seeker.game.personage.models.Characteristics;
import ru.homyakin.seeker.utils.RandomUtils;

public class GroupGenerator implements RaidBattlePersonageGenerator {
    @Override
    public List<BattlePersonage> generate(int personagesCount, double powerMultiplier) {
        final var totalPower = personagesCount * 32000 * (powerMultiplier - 0.06);
        final var group = new ArrayList<BattlePersonage>();
        final var baseCount = personagesCount * 3;
        final int totalCount = RandomUtils.getInInterval((int) (baseCount * 0.9), (int) (baseCount * 1.1));
        final double averagePower = totalPower / totalCount;

        for (int i = 0; i < totalCount; ++i) {
            final double power = RandomUtils.getInInterval((int) (averagePower * 0.9), (int) (averagePower * 1.1));
            final var tempCharacteristics = Characteristics.random();

            final var tempPersonage = new BattlePersonage(
                -1 - i,
                tempCharacteristics,
                null
            );

            final var health = tempPersonage.calculateHealthForTargetPower(power);
            final var characteristics = tempCharacteristics.copyWithHealth((int) health);

            group.add(new BattlePersonage(-1 - i, characteristics, null));
        }
        return group;
    }
}
