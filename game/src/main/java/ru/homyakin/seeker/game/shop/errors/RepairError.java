package ru.homyakin.seeker.game.shop.errors;

import ru.homyakin.seeker.game.models.Money;

public sealed interface RepairError {
    record NotEnoughMoney(Money required) implements RepairError {
    }

    enum NoSuchItem implements RepairError {
        INSTANCE
    }

    enum NotBroken implements RepairError {
        INSTANCE
    }
}
