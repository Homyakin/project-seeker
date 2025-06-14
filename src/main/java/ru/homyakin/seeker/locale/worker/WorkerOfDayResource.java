package ru.homyakin.seeker.locale.worker;

public record WorkerOfDayResource(
    String notEnoughUsers,
    String[] alreadyChosen,
    String chosenUser,
    String[] chosenUserVariations
) {
}
