package ru.homyakin.seeker.telegram.group.stats;

import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.telegram.group.models.GroupId;

public record GroupPersonageStats(
    GroupId groupId,
    PersonageId personageId,
    int raidsSuccess,
    int raidsTotal,
    int duelsWins,
    int duelsTotal,
    long tavernMoneySpent,
    int spinWinsCount
) {
    public GroupPersonageStats addSuccessRaid() {
        return addRaid(true);
    }

    public GroupPersonageStats addFailedRaid() {
        return addRaid(false);
    }

    public GroupPersonageStats addWinDuel() {
        return addDuel(true);
    }

    public GroupPersonageStats addLoseDuel() {
        return addDuel(false);
    }

    public GroupPersonageStats increaseTavernMoneySpent(long amount) {
        return new GroupPersonageStats(
            groupId,
            personageId,
            raidsSuccess,
            raidsTotal,
            duelsWins,
            duelsTotal,
            tavernMoneySpent + amount,
            spinWinsCount
        );
    }

    public GroupPersonageStats addSpinWin() {
        return new GroupPersonageStats(
            groupId,
            personageId,
            raidsSuccess,
            raidsTotal,
            duelsWins,
            duelsTotal,
            tavernMoneySpent,
            spinWinsCount + 1
        );
    }

    private GroupPersonageStats addDuel(boolean isWin) {
        return new GroupPersonageStats(
            groupId,
            personageId,
            raidsSuccess,
            raidsTotal,
            isWin ? duelsWins + 1 : duelsWins,
            duelsTotal + 1,
            tavernMoneySpent,
            spinWinsCount
        );
    }

    private GroupPersonageStats addRaid(boolean isSuccess) {
        return new GroupPersonageStats(
            groupId,
            personageId,
            isSuccess ? raidsSuccess + 1 : raidsSuccess,
            raidsTotal + 1,
            duelsWins,
            duelsTotal,
            tavernMoneySpent,
            spinWinsCount
        );
    }
}
