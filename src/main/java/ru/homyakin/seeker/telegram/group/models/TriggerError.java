package ru.homyakin.seeker.telegram.group.models;

public sealed interface TriggerError {
    enum NoTriggerFound implements TriggerError { INSTANCE }
}
