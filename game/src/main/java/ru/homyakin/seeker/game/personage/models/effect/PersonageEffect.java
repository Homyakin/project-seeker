package ru.homyakin.seeker.game.personage.models.effect;

import java.time.LocalDateTime;

import ru.homyakin.seeker.game.effect.Effect;

public record PersonageEffect(
    Effect effect,
    LocalDateTime expireDateTime
) {
}
