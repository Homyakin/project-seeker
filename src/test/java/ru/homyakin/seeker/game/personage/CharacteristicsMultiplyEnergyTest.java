package ru.homyakin.seeker.game.personage;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.personage.models.Characteristics;
import ru.homyakin.seeker.game.personage.models.Energy;
import ru.homyakin.seeker.utils.TimeUtils;

public class CharacteristicsMultiplyEnergyTest {

    @Test
    public void Given_FullEnergy_When_CharacteristicsMultiply_Then_CharacteristicsAreSame() {
        // given
        final var energy = Energy.createDefault();
        final var characteristics = Characteristics.random();

        // when
        final var result = characteristics.multiply(energy);

        // then
        Assertions.assertEquals(result, characteristics);
    }

    @Test
    public void Given_EmptyEnergy_When_CharacteristicsMultiply_Then_CharacteristicsAreAtMinimum() {
        // given
        final var energy = new Energy(0, TimeUtils.moscowTime());
        final var characteristics = Characteristics.createDefault();

        // when
        final var result = characteristics.multiply(energy);

        // then
        Assertions.assertEquals(250, result.health());
        Assertions.assertEquals(15, result.attack());
        Assertions.assertEquals(6, result.defense());
        Assertions.assertEquals(1, result.strength());
        Assertions.assertEquals(1, result.agility());
        Assertions.assertEquals(1, result.wisdom());
    }

    @Test
    public void Given_EnergyAt60Percent_When_CharacteristicsMultiply_Then_CharacteristicsAreAt60Percent() {
        // given
        final var energy = new Energy(60, TimeUtils.moscowTime());
        final var characteristics = Characteristics.createDefault();

        // when
        final var result = characteristics.multiply(energy);

        // then
        Assertions.assertEquals(300, result.health());
        Assertions.assertEquals(30, result.attack());
        Assertions.assertEquals(12, result.defense());
        Assertions.assertEquals(3, result.strength());
        Assertions.assertEquals(3, result.agility());
        Assertions.assertEquals(3, result.wisdom());
    }

    // TODO отобразить у пользователя, добавить в хелп
}
