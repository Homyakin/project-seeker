package ru.homyakin.seeker.game.season.entity;

public record SeasonNumber(
    int value
) {
    public static SeasonNumber of(int value) {
        return new SeasonNumber(value);
    }
}
