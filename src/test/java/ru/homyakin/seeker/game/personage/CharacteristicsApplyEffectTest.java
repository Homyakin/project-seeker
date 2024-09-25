package ru.homyakin.seeker.game.personage;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.effect.Effect;
import ru.homyakin.seeker.game.effect.EffectCharacteristic;
import ru.homyakin.seeker.game.personage.models.Characteristics;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffect;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffectType;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffects;
import ru.homyakin.seeker.game.tavern_menu.order.models.MenuItemEffect;

import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;

public class CharacteristicsApplyEffectTest {
    @Test
    public void Given_NoEffects_When_Apply_Then_CharacteristicsUnchanged() {
        final var effects = PersonageEffects.EMPTY;
        final var characteristics = new Characteristics(500, 100, 100, 5, 5, 5);
        final var result = characteristics.apply(effects);

        Assertions.assertEquals(result, characteristics);
    }

    @Test
    public void Given_EffectAdd_When_Apply_Then_CharacteristicsAddValueToOneCharacteristic() {
        final var effects = new PersonageEffects(
            Collections.singletonMap(
                PersonageEffectType.MENU_ITEM_EFFECT,
                new PersonageEffect(new Effect.Add(1, EffectCharacteristic.ATTACK), null)
            )
        );
        final var characteristics = new Characteristics(500, 100, 200, 6, 5, 4);
        final var result = characteristics.apply(effects);

        Assertions.assertEquals(result.attack(), characteristics.attack() + 1);
        Assertions.assertEquals(result.health(), characteristics.health());
        Assertions.assertEquals(result.defense(), characteristics.defense());
        Assertions.assertEquals(result.strength(), characteristics.strength());
        Assertions.assertEquals(result.agility(), characteristics.agility());
        Assertions.assertEquals(result.wisdom(), characteristics.wisdom());
    }

    @Test
    public void Given_EffectMultiply_When_Apply_Then_CharacteristicsMultiplyOneCharacteristic() {
        final var effects = new PersonageEffects(
            Collections.singletonMap(
                PersonageEffectType.MENU_ITEM_EFFECT,
                new PersonageEffect(new Effect.Multiplier(1, EffectCharacteristic.HEALTH), null)
            )
        );
        final var characteristics = new Characteristics(500, 100, 200, 6, 5, 4);
        final var result = characteristics.apply(effects);

        Assertions.assertEquals(result.attack(), characteristics.attack());
        Assertions.assertEquals(result.health(), (int) (characteristics.health() * 1.01));
        Assertions.assertEquals(result.defense(), characteristics.defense());
        Assertions.assertEquals(result.strength(), characteristics.strength());
        Assertions.assertEquals(result.agility(), characteristics.agility());
        Assertions.assertEquals(result.wisdom(), characteristics.wisdom());
    }

    @Test
    public void Given_TwoEffectsAdd_When_Apply_Then_CharacteristicsAddValueToTwoCharacteristics() {
        final var effectHashMap = new HashMap<PersonageEffectType, PersonageEffect>();
        effectHashMap.put(
            PersonageEffectType.MENU_ITEM_EFFECT,
            new PersonageEffect(new Effect.Add(1, EffectCharacteristic.ATTACK), null)
        );
        effectHashMap.put(
            PersonageEffectType.THROW_DAMAGE_EFFECT,
            new PersonageEffect(new Effect.Add(1, EffectCharacteristic.WISDOM), null)
        );
        final var effects = new PersonageEffects(
            effectHashMap
        );
        final var characteristics = new Characteristics(500, 100, 200, 6, 5, 4);
        final var result = characteristics.apply(effects);

        Assertions.assertEquals(result.attack(), characteristics.attack() + 1);
        Assertions.assertEquals(result.health(), characteristics.health());
        Assertions.assertEquals(result.defense(), characteristics.defense());
        Assertions.assertEquals(result.strength(), characteristics.strength());
        Assertions.assertEquals(result.agility(), characteristics.agility());
        Assertions.assertEquals(result.wisdom(), characteristics.wisdom() + 1);
    }
}
