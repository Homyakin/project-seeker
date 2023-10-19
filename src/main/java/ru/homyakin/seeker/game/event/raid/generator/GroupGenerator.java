package ru.homyakin.seeker.game.event.raid.generator;

import ru.homyakin.seeker.game.personage.models.Characteristics;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.utils.MathUtils;
import ru.homyakin.seeker.utils.RandomUtils;

import java.util.ArrayList;
import java.util.List;

public class GroupGenerator implements RaidBattleGenerator {
    @Override
    public List<Personage> generate(int personagesCount) {
        // Здесь куча магических цифр и формул полученных методом перебора
        // TODO нормальная генерация
        final var group = new ArrayList<Personage>();
        final int totalCount = (int) ((RandomUtils.getInInterval(200, 300) / 100.0)
            * 3 / Math.max(MathUtils.log(7, personagesCount), 1));

        for (int i = 0; i < personagesCount * totalCount; ++i) {
            final var characteristics = Characteristics.random();
            group.add(
                new Personage(
                    PersonageId.from(-1 - i),
                    null,
                    null,
                    new Characteristics(
                        (int) (RandomUtils.getInInterval(
                            (int) (characteristics.health() * 0.8), (int) (characteristics.health() * 0.9)
                        ) / Math.max(MathUtils.log(1.3, totalCount), 1)),
                        characteristics.attack(),
                        characteristics.defense(),
                        characteristics.strength(),
                        characteristics.agility(),
                        characteristics.wisdom()
                    ),
                    null
                )
            );
        }
        return group;
    }
}
