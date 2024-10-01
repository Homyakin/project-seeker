package ru.homyakin.seeker.locale.raid;

public record RaidResource(
    String joinRaidEvent,
    String raidStartsPrefix,
    String hoursShort,
    String minutesShort,
    String userAlreadyInThisRaid,
    String userAlreadyInOtherEvent,
    String exhaustedAlert,
    String expiredRaid,
    String raidInProcess,
    String[] successRaid,
    String[] failureRaid,
    String[] zeroParticipants,
    String raidResult,
    String[] successItemForPersonage,
    String[] notEnoughSpaceInBagForItem,
    String personageRaidResult,
    String raidParticipants,
    String raidParticipant,
    String report,
    String reportNotPresentForPersonage,
    String shortPersonageReport,
    String lastGroupRaidReportNotFound
) {
}
