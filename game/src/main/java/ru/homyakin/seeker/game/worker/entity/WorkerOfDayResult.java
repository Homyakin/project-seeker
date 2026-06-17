package ru.homyakin.seeker.game.worker.entity;

import ru.homyakin.seeker.game.effect.Effect;
import ru.homyakin.seeker.game.personage.models.Personage;

public record WorkerOfDayResult(
    Personage personage,
    Effect effect
) {
}
