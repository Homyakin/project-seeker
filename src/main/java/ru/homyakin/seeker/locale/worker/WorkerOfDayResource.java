package ru.homyakin.seeker.locale.worker;

public record WorkerOfDayResource(
    String notEnoughMembers,
    String[] alreadyChosen,
    String chosenMember,
    String[] chosenMemberVariations
) {
}
