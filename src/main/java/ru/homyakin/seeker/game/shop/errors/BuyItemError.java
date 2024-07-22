package ru.homyakin.seeker.game.shop.errors;

public sealed interface BuyItemError {
    enum NotEnoughSpaceInBag implements BuyItemError { INSTANCE }

    enum NotEnoughMoney implements BuyItemError { INSTANCE }
}
