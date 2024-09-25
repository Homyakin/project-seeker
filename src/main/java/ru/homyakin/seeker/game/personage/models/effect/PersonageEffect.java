package ru.homyakin.seeker.game.personage.models.effect;

import ru.homyakin.seeker.game.effect.Effect;

import java.time.LocalDateTime;

public record PersonageEffect(
    Effect effect,
    LocalDateTime expireDateTime
) {
}
