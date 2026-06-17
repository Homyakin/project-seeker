package ru.homyakin.seeker.game.event.world_raid.entity;

import ru.homyakin.seeker.game.models.Money;

import java.time.Duration;

public interface WorldRaidConfig {
    Money requiredForDonate();

    int averageRequiredContribution();

    Money fundFromDonation();

    Money fundFromQuest();

    Duration battleDuration();

    int requiredEnergy();

    Duration groupNotificationInterval();

    Money initFund();
}
