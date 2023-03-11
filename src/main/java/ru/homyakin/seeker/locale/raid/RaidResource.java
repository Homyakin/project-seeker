package ru.homyakin.seeker.locale.raid;

public record RaidResource(
    String joinRaidEvent,
    String raidStartsPrefix,
    String hoursShort,
    String minutesShort,
    String successJoinEvent,
    String userAlreadyInThisEvent,
    String userAlreadyInOtherEvent,
    String expiredRaid,
    String[] successRaid,
    String[] failureRaid
) {
}
