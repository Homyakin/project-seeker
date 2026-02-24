package ru.homyakin.seeker.game.event.raid.generator;

import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.personage.models.Characteristics;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffects;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.utils.RandomUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

            group.add(
                new Personage(
                    PersonageId.from(-1 - i),
                    null,
                    Optional.empty(),
                    Optional.empty(),
                    null,
                    characteristics,
                    null,
                    null,
                    Characteristics.ZERO,
                    PersonageEffects.EMPTY
                ).toBattlePersonage()
            );
        }
        return group;
    }
}
