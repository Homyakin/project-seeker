package ru.homyakin.seeker.game.personage;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.personage.models.Characteristics;

public class CharacteristicsRandomTest {

    @Disabled // TODO Рандом проставляет 12 характеристик, может повлиять на баланс
    @Test
    public void When_CreatingRandomCharacteristics_Then_CharacteristicsPointsEquals15_And_OthersAreDefault() {
        // when
        final var result = Characteristics.random();

        // then
        Assertions.assertEquals(15, result.wisdom() + result.strength() + result.agility());
        Assertions.assertEquals(500, result.health());
        Assertions.assertEquals(50, result.attack());
        Assertions.assertEquals(20, result.defense());
    }
}
