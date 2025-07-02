package ru.homyakin.seeker.common.models;

public record BadgeId(
    int value
) {
    public static BadgeId of(int value) {
        return new BadgeId(value);
    }
}
