package ru.homyakin.seeker.game.group.entity;

import ru.homyakin.seeker.game.models.Money;

import java.time.Duration;

public interface GroupConfig {
    EventIntervals defaultEventIntervals();

    Money registrationPrice();

    Money changeTagPrice();

    Duration personageJoinGroupTimeout();
}
