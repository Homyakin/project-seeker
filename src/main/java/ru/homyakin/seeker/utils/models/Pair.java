package ru.homyakin.seeker.utils.models;

public record Pair<T, U>(
    T first,
    U second
) {
    public static <T, U> Pair<T, U> of(T first, U second) {
        return new Pair<>(first, second);
    }
}
