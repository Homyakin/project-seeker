package ru.homyakin.seeker.game.group.entity;

import ru.homyakin.seeker.game.models.Money;

public interface GroupConfig {
    EventIntervals defaultEventIntervals();

    Money registrationPrice();
}
