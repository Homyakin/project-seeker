package ru.homyakin.seeker.game.event.raid.generator;

import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.personage.models.Characteristics;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.PersonageId;

import java.util.Collections;
import java.util.List;

public class SingleBossGenerator implements RaidBattleGenerator {
    @Override
    public List<BattlePersonage> generate(List<BattlePersonage> personages) {
        final var totalPower = personages.stream().mapToDouble(BattlePersonage::power).sum();
        final var tempCharacteristics = Characteristics.random();

        final var tempPersonage = new BattlePersonage(
            -1,
            tempCharacteristics,
            null
        );

        final var health = tempPersonage.calculateHealthForTargetPower(totalPower);
        final var characteristics = tempCharacteristics.copyWithHealth((int) health);
        final var boss = new Personage(
            PersonageId.from(-1),
            null,
            null,
            characteristics,
            null,
            null,
            Characteristics.ZERO
        ).toBattlePersonage();
        return Collections.singletonList(boss);
    }
}
