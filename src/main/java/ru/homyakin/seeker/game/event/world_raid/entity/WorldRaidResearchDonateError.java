package ru.homyakin.seeker.game.event.world_raid.entity;

import ru.homyakin.seeker.game.models.Money;

public sealed interface WorldRaidResearchDonateError {
    record NotEnoughMoney(Money required) implements WorldRaidResearchDonateError {
    }

    enum ResearchCompleted implements WorldRaidResearchDonateError {
        INSTANCE
    }
}
