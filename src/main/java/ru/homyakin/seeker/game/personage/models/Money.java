package ru.homyakin.seeker.game.personage.models;

public record Money(
    int value
) {
    public static Money zero() {
        return new Money(0);
    }
}
