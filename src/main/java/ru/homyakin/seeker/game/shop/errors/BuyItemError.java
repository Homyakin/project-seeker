package ru.homyakin.seeker.game.shop.errors;

import ru.homyakin.seeker.game.models.Money;

public sealed interface BuyItemError {
    enum NotEnoughSpaceInBag implements BuyItemError { INSTANCE }

    record NotEnoughMoney(Money required) implements BuyItemError {
    }
}
