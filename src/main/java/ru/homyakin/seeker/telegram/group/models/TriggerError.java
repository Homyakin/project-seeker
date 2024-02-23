package ru.homyakin.seeker.telegram.group.models;

public sealed interface TriggerError {
    enum IncorrectHour implements TriggerError { INSTANCE }
}
