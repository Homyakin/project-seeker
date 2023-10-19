package ru.homyakin.seeker.telegram.group.models;

public record GroupStats(
    GroupId groupId,
    int raidsComplete,
    int duelsComplete,
    long tavernMoneySpent
) {}
