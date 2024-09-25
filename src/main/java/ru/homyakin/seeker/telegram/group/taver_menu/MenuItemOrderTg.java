package ru.homyakin.seeker.telegram.group.taver_menu;

import ru.homyakin.seeker.telegram.group.models.GroupId;

public record MenuItemOrderTg(
    long menuItemOrderId,
    GroupId groupTgId,
    int messageId
) {}

