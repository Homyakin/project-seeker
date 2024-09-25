package ru.homyakin.seeker.game.personage.models.effect;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.vavr.control.Either;
import ru.homyakin.seeker.game.personage.models.errors.StillSame;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public record PersonageEffects(
    Map<PersonageEffectType, PersonageEffect> effects
) {
    public PersonageEffects addEffect(PersonageEffectType type, PersonageEffect effect) {
        final var newEffects = new HashMap<>(effects);
        newEffects.put(type, effect);
        return new PersonageEffects(newEffects);
    }

    public Either<StillSame, PersonageEffects> expireIfNeeded(LocalDateTime now) {
        final var newEffects = new HashMap<PersonageEffectType, PersonageEffect>();
        for (final var entry : effects.entrySet()) {
            final var effect = entry.getValue();
            if (effect.expireDateTime().isAfter(now)) {
                newEffects.put(entry.getKey(), effect);
            }
        }
        if (newEffects.size() == effects.size()) {
            return Either.left(StillSame.INSTANCE);
        }
        return Either.right(new PersonageEffects(newEffects));
    }

    @JsonIgnore
    public boolean isEmpty() {
        return effects.isEmpty();
    }

    public static final PersonageEffects EMPTY = new PersonageEffects(Collections.emptyMap());
}
