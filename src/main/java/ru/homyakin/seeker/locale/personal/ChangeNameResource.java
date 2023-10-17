package ru.homyakin.seeker.locale.personal;

public record ChangeNameResource(
    String changeNameWithoutName,
    String initChangeName,
    String cancelChangeName,
    String confirmName,
    String cancelButton,
    String confirmButton,
    String repeatButton,
    String personageNameInvalidLength,
    String personageNameInvalidSymbols,
    String successNameChange,
    String internalError
) {
}
