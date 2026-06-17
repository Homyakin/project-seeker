package ru.homyakin.seeker.game.personage;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffect;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffectType;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffects;
import ru.homyakin.seeker.game.personage.models.errors.StillSame;
import ru.homyakin.seeker.game.tavern_menu.order.models.MenuItemEffect;
import ru.homyakin.seeker.utils.TimeUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;

public class PersonageEffectsExpireTest {
    @Test
    public void Given_EmptyEffects_When_Expire_Then_StillSame() {
        final var effects = PersonageEffects.EMPTY;
        final var result = effects.expireIfNeeded(TimeUtils.moscowTime());

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(StillSame.INSTANCE, result.getLeft());
    }

    @Test
    public void Given_NonEmptyEffects_When_ExpireBeforeExpireTime_Then_StillSame() {
        final var now = TimeUtils.moscowTime();
        final var effects = new PersonageEffects(
            Collections.singletonMap(
                PersonageEffectType.MENU_ITEM_EFFECT,
                new PersonageEffect(null, now.plusHours(1))
            )
        );
        final var result = effects.expireIfNeeded(TimeUtils.moscowTime());

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(StillSame.INSTANCE, result.getLeft());
    }

    @Test
    public void Given_TwoEffects_When_ExpireOfOneIsAfterExpireTime_Then_RemainOneEffect() {
        final var now = TimeUtils.moscowTime();
        final var effectsHashMap = new HashMap<PersonageEffectType, PersonageEffect>();
        effectsHashMap.put(
            PersonageEffectType.MENU_ITEM_EFFECT,
            new PersonageEffect(null, now.minusHours(1))
        );
        effectsHashMap.put(
            PersonageEffectType.THROW_DAMAGE_EFFECT,
            new PersonageEffect(null, now.plusHours(1))
        );
        final var effects = new PersonageEffects(effectsHashMap);
        final var result = effects.expireIfNeeded(TimeUtils.moscowTime());

        Assertions.assertTrue(result.isRight());
        Assertions.assertEquals(1, result.get().effects().size());
        Assertions.assertEquals(
            new PersonageEffect(null, now.plusHours(1)),
            result.get().effects().get(PersonageEffectType.THROW_DAMAGE_EFFECT)
        );
    }
}
