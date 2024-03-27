package ru.homyakin.seeker.game.item.models;

public sealed interface GenerateItemError {
    record NotEnoughSpace(Item item) implements GenerateItemError {}
}
