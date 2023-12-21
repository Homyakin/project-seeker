package ru.homyakin.seeker.game.duel.models;

import ru.homyakin.seeker.game.models.Money;

public sealed interface CreateDuelError {
    final class PersonageAlreadyHasDuel implements CreateDuelError {
    }

    record InitiatingPersonageNotEnoughMoney(Money money) implements CreateDuelError {
    }

    final class InternalError implements CreateDuelError {
    }
}

