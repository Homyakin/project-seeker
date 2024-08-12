package ru.homyakin.seeker.game.personage;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.personage.models.PersonageEffects;
import ru.homyakin.seeker.game.personage.models.errors.StillSame;
import ru.homyakin.seeker.game.tavern_menu.models.MenuItemEffect;
import ru.homyakin.seeker.utils.TimeUtils;

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
            Optional.of(new MenuItemEffect(null, now.plusHours(1)))
        );
        final var result = effects.expireIfNeeded(TimeUtils.moscowTime());

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(StillSame.INSTANCE, result.getLeft());
    }

    @Test
    public void Given_NonEmptyEffects_When_ExpireAfterExpireTime_Then_EmptyEffect() {
        final var now = TimeUtils.moscowTime();
        final var effects = new PersonageEffects(
            Optional.of(new MenuItemEffect(null, now.minusHours(1)))
        );
        final var result = effects.expireIfNeeded(TimeUtils.moscowTime());

        Assertions.assertTrue(result.isRight());
        Assertions.assertTrue(result.get().menuItemEffect().isEmpty());
    }
}
