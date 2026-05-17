package ru.homyakin.seeker.game.personage;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.personage.models.Characteristics;

public class CharacteristicsRandomTest {

    @Test
    public void When_CreatingRandomCharacteristics_Then_ReturnDefaultValues() {
        final var result = Characteristics.random();

        Assertions.assertEquals(500, result.health());
        Assertions.assertEquals(50, result.attack());
        Assertions.assertEquals(20, result.defense());
    }
}
