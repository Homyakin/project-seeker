package ru.homyakin.seeker.game.shop.models;

import ru.homyakin.seeker.game.models.Money;

public sealed interface EnhanceAction {
    record AddModifier(Money price) implements EnhanceAction {
    }
}
