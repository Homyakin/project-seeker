package ru.homyakin.seeker.game.battle;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.effect.Effect;
import ru.homyakin.seeker.game.effect.EffectCharacteristic;
import ru.homyakin.seeker.game.personage.models.Characteristics;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.PersonageEffects;
import ru.homyakin.seeker.game.tavern_menu.models.MenuItemEffect;

import java.util.Optional;

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

    @Test
    public void Given_PersonageWithEffect_When_CalculatePower_Then_EffectsDontChangePower() {
        final var personage = new BattlePersonage(
            0,
            new Characteristics(500, 50, 20, 5, 5, 5),
            new Personage(
                null,
                null,
                null,
                new Characteristics(500, 50, 20, 5, 5, 5),
                null,
                null,
                Characteristics.ZERO,
                new PersonageEffects(Optional.of(new MenuItemEffect(new Effect.Add(100, EffectCharacteristic.ATTACK), null))),
                Optional.empty()
            )
        );

        final var power = (int) personage.power();

        Assertions.assertEquals(32052, power);
    }
}
