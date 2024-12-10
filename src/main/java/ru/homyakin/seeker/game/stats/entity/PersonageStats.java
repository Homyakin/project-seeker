package ru.homyakin.seeker.game.stats.entity;

public record PersonageStats(
    int raidsSuccess,
    int raidsTotal,
    int duelsWins,
    int duelsTotal,
    long tavernMoneySpent,
    int spinWinsCount,
    int questsSuccess,
    int questsTotal
) {
}
