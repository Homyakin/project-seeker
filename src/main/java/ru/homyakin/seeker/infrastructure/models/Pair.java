package ru.homyakin.seeker.infrastructure.models;

public record Pair<T, U>(
    T first,
    U second
) {
}
