package ru.homyakin.seeker.locale.raid;

public record RaidResource(
    String joinRaidEvent,
    String hoursShort,
    String minutesShort,
    String userAlreadyInThisRaid,
    String userAlreadyInOtherEvent,
    String exhaustedAlert,
    String expiredRaid,
    String raidInProcess,
    String successJoinRaid,
    String[] successRaid,
    String[] failureRaid,
    String[] zeroParticipants,
    String raidResult,
    String[] successItemForPersonage,
    String[] successContrabandForPersonage,
    String[] notEnoughSpaceInBagForItem,
    String personageRaidResult,
    String raidParticipants,
    String raidParticipant,
    String report,
    String reportNotPresentForPersonage,
    String lastGroupRaidReportNotFound,
    String raidBaseMessage,
    String raidStarting
) {
}
