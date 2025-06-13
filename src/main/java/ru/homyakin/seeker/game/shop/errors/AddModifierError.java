package ru.homyakin.seeker.game.shop.errors;

import ru.homyakin.seeker.game.models.Money;

public sealed interface AddModifierError {
    record NotEnoughMoney(Money required) implements AddModifierError {
    }

    enum NoSuchItem implements AddModifierError {
        INSTANCE
    }

    enum MaxModifiers implements AddModifierError {
        INSTANCE
    }

    enum ItemIsBroken implements AddModifierError {
        INSTANCE
    }
}
