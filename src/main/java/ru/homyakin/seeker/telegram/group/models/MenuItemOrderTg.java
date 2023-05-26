package ru.homyakin.seeker.telegram.group.models;

public record MenuItemOrderTg(
    long menuItemOrderId,
    long groupTgId,
    int messageId
) {}

