package ru.homyakin.seeker.locale.raid;

public record RaidResource(
    String joinRaidEvent,
    String raidStartsPrefix,
    String hoursShort,
    String minutesShort,
    String userAlreadyInThisEvent,
    String userAlreadyInOtherEvent,
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
    String notEnoughEnergy,
    String report,
    String reportNotPresentForPersonage,
    String shortPersonageReport,
    String lastGroupRaidReportNotFound
) {
}
