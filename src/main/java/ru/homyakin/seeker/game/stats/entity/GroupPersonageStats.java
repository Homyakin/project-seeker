package ru.homyakin.seeker.game.stats.entity;

import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.season.entity.SeasonNumber;

public record GroupPersonageStats(
    SeasonNumber seasonNumber,
    GroupId groupId,
    PersonageId personageId,
    int raidsSuccess,
    int raidsTotal,
    int duelsWins,
    int duelsTotal,
    long tavernMoneySpent,
    int workerOfDayCount
) {
    public static GroupPersonageStats withSuccessRaid(SeasonNumber seasonNumber, GroupId groupId, PersonageId personageId) {
        return new GroupPersonageStats(
            seasonNumber,
            groupId,
            personageId,
            1,
            1,
            0,
            0,
            0,
            0
        );
    }

    public static GroupPersonageStats withFailedRaid(SeasonNumber seasonNumber, GroupId groupId, PersonageId personageId) {
        return new GroupPersonageStats(
            seasonNumber,
            groupId,
            personageId,
            0,
            1,
            0,
            0,
            0,
            0
        );
    }

    public static GroupPersonageStats withWinDuel(SeasonNumber seasonNumber, GroupId groupId, PersonageId personageId) {
        return new GroupPersonageStats(
            seasonNumber,
            groupId,
            personageId,
            0,
            0,
            1,
            1,
            0,
            0
        );
    }

    public static GroupPersonageStats withLoseDuel(SeasonNumber seasonNumber, GroupId groupId, PersonageId personageId) {
        return new GroupPersonageStats(
            seasonNumber,
            groupId,
            personageId,
            0,
            0,
            0,
            1,
            0,
            0
        );
    }

    public static GroupPersonageStats withSpentTavernMoney(
        SeasonNumber seasonNumber,
        GroupId groupId,
        PersonageId personageId,
        Money money
    ) {
        return new GroupPersonageStats(
            seasonNumber,
            groupId,
            personageId,
            0,
            0,
            0,
            0,
            money.value(),
            0
        );
    }

    public static GroupPersonageStats withWorkerOfDay(SeasonNumber seasonNumber, GroupId groupId, PersonageId personageId) {
        return new GroupPersonageStats(
            seasonNumber,
            groupId,
            personageId,
            0,
            0,
            0,
            0,
            0,
            1
        );
    }
}
