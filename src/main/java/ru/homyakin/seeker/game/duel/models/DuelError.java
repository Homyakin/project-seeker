package ru.homyakin.seeker.game.duel.models;

import ru.homyakin.seeker.game.models.Money;

public sealed interface DuelError {
    final class PersonageAlreadyHasDuel implements DuelError {
    }

    record InitiatingPersonageNotEnoughMoney(Money money) implements DuelError {
    }

    final class InternalError implements DuelError {
    }
}

