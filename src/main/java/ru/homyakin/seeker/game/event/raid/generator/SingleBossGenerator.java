package ru.homyakin.seeker.game.event.raid.generator;

import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.personage.models.Characteristics;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffects;
import ru.homyakin.seeker.game.personage.models.PersonageId;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class SingleBossGenerator implements RaidBattlePersonageGenerator {
    @Override
    public List<BattlePersonage> generate(List<BattlePersonage> personages, double powerPercent) {
        final var totalPower = personages.stream().mapToDouble(BattlePersonage::power).sum() * powerPercent;
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
            null,
            characteristics,
            null,
            null,
            Characteristics.ZERO,
            PersonageEffects.EMPTY,
            Optional.empty()
        ).toBattlePersonage();
        return Collections.singletonList(boss);
    }
}
