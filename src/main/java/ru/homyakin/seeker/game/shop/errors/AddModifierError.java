package ru.homyakin.seeker.game.shop.errors;

import ru.homyakin.seeker.game.models.Money;

public sealed interface AddModifierError {
    record NotEnoughMoney(Money required) implements AddModifierError {
    }

    enum NoSuchItem implements AddModifierError {
        INSTANCE
    }

    enum MaxRarity implements AddModifierError {
        INSTANCE
    }
}
