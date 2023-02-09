package ru.homyakin.seeker.locale.personal;

public record ChangeNameResource(
    String changeNameWithoutName,
    String personageNameInvalidLength,
    String personageNameInvalidSymbols,
    String successNameChange
) {
}
