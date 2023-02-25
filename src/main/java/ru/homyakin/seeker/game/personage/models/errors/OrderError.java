package ru.homyakin.seeker.game.personage.models.errors;

import ru.homyakin.seeker.game.models.Money;

public sealed interface OrderError {
    record NotAvailableItem() implements OrderError {}
    record NotEnoughMoney(Money itemCost, Money personageMoney) implements OrderError {}

}
