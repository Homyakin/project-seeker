package ru.homyakin.seeker.telegram.group.taver_menu;

import ru.homyakin.seeker.telegram.group.models.GroupTgId;

public record MenuItemOrderTg(
    long menuItemOrderId,
    GroupTgId groupTgId,
    int messageId
) {}

