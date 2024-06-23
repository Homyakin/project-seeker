package ru.homyakin.seeker.game.battle;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.personage.models.Characteristics;

public class BattlePersonagePowerTest {
    @Test
    public void When_CalculatePower_Then_CorrectValueGraterThanZero() {
        final var personage = new BattlePersonage(
            0,
            new Characteristics(500, 50, 20, 5, 5, 5),
            null
        );

        final var power = (int) personage.power();

        Assertions.assertEquals(32052, power);
    }

    @Test
    public void Given_CalculatedPower_When_CalculateHealthFromPower_Then_ReturnSameHealth() {
        final var personage = new BattlePersonage(
            0,
            new Characteristics(500, 50, 20, 5, 5, 5),
            null
        );

        final var power = personage.power();

        Assertions.assertEquals(500, (int) personage.calculateHealthForTargetPower(power));
    }
}
