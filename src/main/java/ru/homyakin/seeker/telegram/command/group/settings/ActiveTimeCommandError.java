package ru.homyakin.seeker.telegram.command.group.settings;

public sealed interface ActiveTimeCommandError {
    enum IncorrectArgumentsNumber implements ActiveTimeCommandError { INSTANCE }

    enum ArgumentsNotANumber implements ActiveTimeCommandError { INSTANCE }
}
