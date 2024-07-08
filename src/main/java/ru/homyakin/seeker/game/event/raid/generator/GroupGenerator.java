package ru.homyakin.seeker.game.event.raid.generator;

import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.personage.models.Characteristics;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.utils.MathUtils;
import ru.homyakin.seeker.utils.RandomUtils;

import java.util.ArrayList;
import java.util.List;

public class GroupGenerator implements RaidBattlePersonageGenerator {
    @Override
    public List<BattlePersonage> generate(List<BattlePersonage> personages, double powerPercent) {
        final var totalPower = personages.stream().mapToDouble(BattlePersonage::power).sum()
            / calcPowerPenalty(personages.size()) * powerPercent;
        final var group = new ArrayList<BattlePersonage>();
        final var baseCount = personages.size() * 3;
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
                    null,
                    characteristics,
                    null,
                    null,
                    Characteristics.ZERO
                ).toBattlePersonage()
            );
        }
        return group;
    }

    /**
     * Считаем штраф для мощи группы противников. Иначе они будут слишком сильными на большом количестве персонажей
     * Примерные значения при количестве персонажей:
     * 1 => 1.05
     * 5 => 1.08
     * 15 => 1.1
     */
    private double calcPowerPenalty(int personagesCount) {
        return MathUtils.calcOneDivideXFunc(personagesCount, -0.392, -4.6, 1.12);
    }
}
