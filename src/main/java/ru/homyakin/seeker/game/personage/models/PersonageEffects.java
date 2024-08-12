package ru.homyakin.seeker.game.personage.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.vavr.control.Either;
import ru.homyakin.seeker.game.personage.models.errors.StillSame;
import ru.homyakin.seeker.game.tavern_menu.models.MenuItemEffect;

import java.time.LocalDateTime;
import java.util.Optional;

public record PersonageEffects(
    Optional<MenuItemEffect> menuItemEffect
) {
    public PersonageEffects addMenuItemEffect(MenuItemEffect effect) {
        return new PersonageEffects(
            Optional.of(effect)
        );
    }

    public Either<StillSame, PersonageEffects> expireIfNeeded(LocalDateTime now) {
        if (menuItemEffect.isEmpty()) {
            return Either.left(StillSame.INSTANCE);
        }
        if (menuItemEffect.get().expireDateTime().isBefore(now)) {
            return Either.right(new PersonageEffects(Optional.empty()));
        }
        return Either.left(StillSame.INSTANCE);
    }

    @JsonIgnore
    public boolean isEmpty() {
        return menuItemEffect.isEmpty();
    }

    public static final PersonageEffects EMPTY = new PersonageEffects(
        Optional.empty()
    );
}
