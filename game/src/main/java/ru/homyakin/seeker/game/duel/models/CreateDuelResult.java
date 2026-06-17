package ru.homyakin.seeker.game.duel.models;

import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.models.Personage;

public record CreateDuelResult(
    long duelId,
    Personage initiatingPersonage,
    Personage acceptingPersonage,
    Money cost
) {
}
