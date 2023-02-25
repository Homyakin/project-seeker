package ru.homyakin.seeker.game.personage.models.errors;

public sealed interface OrderError {
    record NotAvailableItem() implements OrderError {}
    record NotEnoughMoney(int itemCost, int personageMoney) implements OrderError {}

}
