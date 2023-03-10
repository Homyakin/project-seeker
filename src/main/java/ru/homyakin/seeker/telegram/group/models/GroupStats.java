package ru.homyakin.seeker.telegram.group.models;

public record GroupStats(
    long groupId,
    int raidsComplete,
    int duelsComplete,
    long tavernMoneySpent
) {}
