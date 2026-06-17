package ru.homyakin.seeker.telegram.group.duel;

import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.telegram.models.TgPersonageMention;

public record CreateDuelTgResult(
    long duelId,
    TgPersonageMention initiator,
    TgPersonageMention acceptor,
    Money cost
) {
}
