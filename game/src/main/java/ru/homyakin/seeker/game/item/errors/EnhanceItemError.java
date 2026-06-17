package ru.homyakin.seeker.game.item.errors;

public sealed interface EnhanceItemError {
    enum MaxRarity implements EnhanceItemError { INSTANCE }
}
