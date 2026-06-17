package ru.homyakin.seeker.game.personage.models.errors;

import ru.homyakin.seeker.game.models.Money;

public record NotEnoughMoney(
    Money neededMoney
) {
}
