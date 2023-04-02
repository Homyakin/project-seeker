package ru.homyakin.seeker.telegram.group.models;

public sealed interface ActiveTimeError {
    enum IncorrectHour implements ActiveTimeError { INSTANCE }

    enum StartMoreThanEnd implements ActiveTimeError { INSTANCE }

    record IncorrectTimeZone(int min, int max) implements ActiveTimeError {}
}
