package ru.homyakin.seeker.telegram.command.group.trigger;

public sealed interface TriggerCommandError {

    enum NoTriggerTextCommandError implements TriggerCommandError { INSTANCE }

}
