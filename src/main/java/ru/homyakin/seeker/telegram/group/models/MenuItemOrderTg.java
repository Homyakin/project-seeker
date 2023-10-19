package ru.homyakin.seeker.telegram.group.models;

public record MenuItemOrderTg(
    long menuItemOrderId,
    GroupId groupTgId,
    int messageId
) {}

