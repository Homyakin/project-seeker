package ru.homyakin.seeker.game.stats.entity;

import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.season.entity.SeasonNumber;

public record GroupStats(
    SeasonNumber seasonNumber,
    GroupId groupId,
    int raidsSuccess,
    int raidsTotal,
    int duelsComplete,
    long tavernMoneySpent,
    int worldRaidsSuccess,
    int worldRaidsTotal
) {

    public static GroupStats withSuccessRaid(SeasonNumber seasonNumber, GroupId groupId) {
        return new GroupStats(
            seasonNumber,
            groupId,
            1,
            1,
            0,
            0,
           0,
            0
        );
    }

    public static GroupStats withFailedRaid(SeasonNumber seasonNumber, GroupId groupId) {
        return new GroupStats(
            seasonNumber,
            groupId,
            0,
            1,
            0,
            0,
            0,
            0
        );
    }

    public static GroupStats withCompleteDuel(SeasonNumber seasonNumber, GroupId groupId) {
        return new GroupStats(
            seasonNumber,
            groupId,
            0,
            0,
            1,
            0,
            0,
            0
        );
    }

    public static GroupStats withSpentTavernMoney(
        SeasonNumber seasonNumber,
        GroupId groupId,
        Money tavernMoneySpent
    ) {
        return new GroupStats(
            seasonNumber,
            groupId,
            0,
            0,
            0,
            tavernMoneySpent.value(),
            0,
            0
        );
    }

    public static GroupStats withSuccessWorldRaid(
        SeasonNumber seasonNumber,
        GroupId groupId
    ) {
        return new GroupStats(
            seasonNumber,
            groupId,
            0,
            0,
           0,
            0,
            1,
            1
        );
    }

    public static GroupStats withFailedWorldRaid(
        SeasonNumber seasonNumber,
        GroupId groupId
    ) {
        return new GroupStats(
            seasonNumber,
            groupId,
            0,
            0,
            0,
            0,
            0,
            1
        );
    }
}
